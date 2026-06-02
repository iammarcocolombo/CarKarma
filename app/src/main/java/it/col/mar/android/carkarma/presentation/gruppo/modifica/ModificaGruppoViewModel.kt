package it.col.mar.android.carkarma.presentation.gruppo.modifica

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.col.mar.android.carkarma.domain.repository.AmicoRepository
import it.col.mar.android.carkarma.domain.repository.GruppoRepository
import it.col.mar.android.carkarma.data.model.Amico
import it.col.mar.android.carkarma.data.model.Gruppo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel per la creazione e la modifica di un Gruppo.
 * Totalmente ripulito da dipendenze dirette con Firebase (Clean Architecture).
 * Interagisce unicamente con le astrazioni (interfacce) dei repository del Domain Layer.
 */
class ModificaGruppoViewModel(
    private val gruppoRepository: GruppoRepository,
    private val amicoRepository: AmicoRepository
) : ViewModel() {

    private val _nomeGruppo = MutableStateFlow("")
    val nomeGruppo: StateFlow<String> = _nomeGruppo

    private val _selectedAvatarIndex = MutableStateFlow(0)
    val selectedAvatarIndex: StateFlow<Int> = _selectedAvatarIndex

    // Otteniamo la lista completa degli amici della rubrica personale
    val amiciDisponibili: StateFlow<List<Amico>> = amicoRepository.amici

    private val _amiciSelezionatiIds = MutableStateFlow<Set<String>>(emptySet())
    val amiciSelezionati: StateFlow<Set<String>> = _amiciSelezionatiIds

    private var currentGruppoId: String = ""
    private var currentUtentiIds: List<String> = emptyList()

    fun loadGruppo(gruppoId: String) {
        this.currentGruppoId = gruppoId
        viewModelScope.launch {
            if (gruppoId.isNotEmpty()) {
                val gruppo = gruppoRepository.getGruppoPerId(gruppoId)
                if (gruppo != null) {
                    _nomeGruppo.value = gruppo.nome
                    _selectedAvatarIndex.value = gruppo.avatarIndex
                    currentUtentiIds = gruppo.utentiIds

                    // Carichiamo i membri attuali del gruppo per popolare le checkbox
                    val membriAttuali = gruppoRepository.getMembriDelGruppo(gruppoId).first()
                    _amiciSelezionatiIds.value = membriAttuali.map { it.id }.toSet()
                }
            } else {
                _nomeGruppo.value = ""
                _selectedAvatarIndex.value = 0
                _amiciSelezionatiIds.value = emptySet()
                currentUtentiIds = emptyList()
            }
        }
    }

    fun onNomeGruppoChange(v: String) { _nomeGruppo.value = v }
    fun onAvatarSelected(index: Int) { _selectedAvatarIndex.value = index }

    fun toggleAmicoSelezionato(amicoId: String) {
        _amiciSelezionatiIds.value = _amiciSelezionatiIds.value.toMutableSet().apply {
            if (contains(amicoId)) remove(amicoId) else add(amicoId)
        }
    }

    fun salvaGruppo(onSalvato: () -> Unit) {
        viewModelScope.launch {
            // Generiamo l'id tramite il repository delegato
            val idFinale = currentGruppoId.ifEmpty { gruppoRepository.generaNuovoId() }

            val gruppo = Gruppo(
                id = idFinale,
                nome = _nomeGruppo.value,
                membriIds = _amiciSelezionatiIds.value.toList(),
                utentiIds = currentUtentiIds,
                avatarIndex = _selectedAvatarIndex.value
            )

            // 1. Salviamo l'intestazione del Gruppo tramite interfaccia di Dominio
            if (currentGruppoId.isEmpty()) {
                gruppoRepository.aggiungiGruppo(gruppo)
            } else {
                gruppoRepository.aggiornaGruppo(gruppo)
            }

            // 2. Allineamento dei membri (sottocollezione)
            val tuttiStampini = amicoRepository.amici.value
            val idsUI = _amiciSelezionatiIds.value

            if (currentGruppoId.isEmpty()) {
                // Nuovi iscritti per il nuovo gruppo
                idsUI.forEach { id ->
                    tuttiStampini.find { it.id == id }?.let { template ->
                        gruppoRepository.aggiungiMembroAlGruppo(idFinale, template)
                    }
                }
            } else {
                // Calcoliamo la differenza rispetto a chi è già sul database
                val membriNelDb = gruppoRepository.getMembriDelGruppo(idFinale).first().map { it.id }.toSet()

                // Selezionati nella UI ma non nel DB -> Vengono aggiunti (nuove istanze a km 0)
                idsUI.filter { !membriNelDb.contains(it) }.forEach { id ->
                    tuttiStampini.find { it.id == id }?.let { template ->
                        gruppoRepository.aggiungiMembroAlGruppo(idFinale, template)
                    }
                }

                // Presenti nel DB ma non più nella UI -> Vengono rimossi in modo sicuro
                membriNelDb.filter { !idsUI.contains(it) }.forEach { id ->
                    gruppoRepository.rimuoviMembroDalGruppo(idFinale, id)
                }
            }

            delay(100)
            onSalvato()
        }
    }

    fun eliminaGruppo(onEliminato: () -> Unit) {
        viewModelScope.launch {
            if (currentGruppoId.isNotEmpty()) {
                gruppoRepository.eliminaGruppo(currentGruppoId)
                delay(200)
            }
            onEliminato()
        }
    }
}