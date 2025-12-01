package it.col.mar.android.carkarma.presentation.gruppo.modifica

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.col.mar.android.carkarma.data.database.AmicoRepository
import it.col.mar.android.carkarma.data.database.GruppoRepository
import it.col.mar.android.carkarma.data.model.Amico
import it.col.mar.android.carkarma.data.model.Gruppo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class ModificaGruppoViewModel(
    private val gruppoRepository: GruppoRepository,
    private val amicoRepository: AmicoRepository
) : ViewModel() {

    private val _nomeGruppo = MutableStateFlow("")
    val nomeGruppo: StateFlow<String> = _nomeGruppo

    // Gli "stampini" dalla rubrica globale (lista completa per la selezione)
    val amiciDisponibili: StateFlow<List<Amico>> = amicoRepository.amici

    // Set degli ID degli amici selezionati nella UI
    private val _amiciSelezionatiIds = MutableStateFlow<Set<String>>(emptySet())
    val amiciSelezionati: StateFlow<Set<String>> = _amiciSelezionatiIds

    private var currentGruppoId: String = ""

    fun loadGruppo(gruppoId: String) {
        this.currentGruppoId = gruppoId
        viewModelScope.launch {
            if (gruppoId.isNotEmpty()) {
                val gruppo = gruppoRepository.getGruppoPerId(gruppoId)
                if (gruppo != null) {
                    _nomeGruppo.value = gruppo.nome

                    // Carichiamo i membri che sono GIA' nel gruppo per pre-spuntare le checkbox.
                    // Usiamo .first() per prendere un'istantanea dei membri attuali.
                    val membriAttuali = gruppoRepository.getMembriDelGruppo(gruppoId).first()
                    _amiciSelezionatiIds.value = membriAttuali.map { it.id }.toSet()
                }
            } else {
                // Nuovo gruppo: tutto vuoto
                _nomeGruppo.value = ""
                _amiciSelezionatiIds.value = emptySet()
            }
        }
    }

    fun onNomeGruppoChange(nuovoNome: String) {
        _nomeGruppo.value = nuovoNome
    }

    fun toggleAmicoSelezionato(amicoId: String) {
        _amiciSelezionatiIds.value = _amiciSelezionatiIds.value.toMutableSet().apply {
            if (contains(amicoId)) remove(amicoId) else add(amicoId)
        }
    }

    fun salvaGruppo(onSalvato: () -> Unit) {
        viewModelScope.launch {
            // Generiamo l'ID qui se è nuovo, così possiamo usarlo sia per il gruppo che per i membri
            val idFinale = if (currentGruppoId.isEmpty()) UUID.randomUUID().toString() else currentGruppoId

            // 1. Salviamo il documento del Gruppo (Intestazione)
            // Salviamo anche la lista degli ID per comodità di visualizzazione nella Home
            val gruppo = Gruppo(
                id = idFinale,
                nome = _nomeGruppo.value,
                membriIds = _amiciSelezionatiIds.value.toList()
            )
            gruppoRepository.aggiungiGruppo(gruppo)

            // 2. Gestione intelligente dei Membri (Sottocollezione)
            val tuttiStampini = amicoRepository.amici.value
            val idsSelezionatiUI = _amiciSelezionatiIds.value

            if (currentGruppoId.isEmpty()) {
                // CASO A: NUOVO GRUPPO -> Aggiungiamo tutti i selezionati (nuove istanze km 0)
                idsSelezionatiUI.forEach { id ->
                    val template = tuttiStampini.find { it.id == id }
                    if (template != null) {
                        gruppoRepository.aggiungiMembroAlGruppo(idFinale, template)
                    }
                }
            } else {
                // CASO B: GRUPPO ESISTENTE -> Dobbiamo fare un "diff" (differenza)
                // Recuperiamo gli ID di chi è GIA' salvato nel DB del gruppo
                val membriNelDbIds = gruppoRepository.getMembriDelGruppo(idFinale).first().map { it.id }.toSet()

                // 1. Chi c'è nella UI ma NON nel DB? -> Sono NUOVI membri -> Li aggiungiamo (km 0)
                val nuoviMembriIds = idsSelezionatiUI.filter { !membriNelDbIds.contains(it) }
                nuoviMembriIds.forEach { id ->
                    val template = tuttiStampini.find { it.id == id }
                    if (template != null) {
                        gruppoRepository.aggiungiMembroAlGruppo(idFinale, template)
                    }
                }

                // 2. Chi c'era nel DB ma NON è più nella UI? -> Sono RIMOSSI -> Li cancelliamo
                val rimossiMembriIds = membriNelDbIds.filter { !idsSelezionatiUI.contains(it) }
                rimossiMembriIds.forEach { id ->
                    gruppoRepository.rimuoviMembroDalGruppo(idFinale, id)
                }

                // 3. Chi c'è in entrambi? -> NON FACCIAMO NULLA!
                // Questo è il trucco: non toccando i membri esistenti, preserviamo i loro km accumulati.
            }

            onSalvato()
        }
    }

    fun eliminaGruppo(onEliminato: () -> Unit) {
        viewModelScope.launch {
            if (currentGruppoId.isNotEmpty()) {
                gruppoRepository.eliminaGruppo(currentGruppoId)
            }
            onEliminato()
        }
    }
}