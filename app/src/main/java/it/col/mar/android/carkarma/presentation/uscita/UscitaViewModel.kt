package it.col.mar.android.carkarma.presentation.uscita

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.col.mar.android.carkarma.data.database.AmicoRepository
import it.col.mar.android.carkarma.data.database.GruppoRepository
import it.col.mar.android.carkarma.data.database.UscitaRepository
import it.col.mar.android.carkarma.data.model.Amico
import it.col.mar.android.carkarma.data.model.Uscita
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UscitaViewModel(
    private val uscitaRepository: UscitaRepository,
    private val amicoRepository: AmicoRepository,
    private val gruppoRepository: GruppoRepository // Aggiunto per filtrare gli amici
) : ViewModel() {

    private var currentUscitaId: String = ""
    private var currentGruppoId: String = ""

    private val _nomeUscita = MutableStateFlow("")
    val nomeUscita: StateFlow<String> = _nomeUscita

    // Lista filtrata: solo gli amici che appartengono a questo gruppo
    private val _amiciDelGruppo = MutableStateFlow<List<Amico>>(emptyList())
    val amiciDelGruppo: StateFlow<List<Amico>> = _amiciDelGruppo

    // Set di ID String
    private val _partecipantiSelezionati = MutableStateFlow<Set<String>>(emptySet())
    val partecipantiSelezionati: StateFlow<Set<String>> = _partecipantiSelezionati

    private val _guidatoriSelezionati = MutableStateFlow<Set<String>>(emptySet())
    val guidatoriSelezionati: StateFlow<Set<String>> = _guidatoriSelezionati

    private val _kmTotali = MutableStateFlow(0)
    val kmTotali: StateFlow<Int> = _kmTotali

    fun loadUscita(gruppoId: String, uscitaId: String) {
        this.currentGruppoId = gruppoId
        this.currentUscitaId = uscitaId

        viewModelScope.launch {
            // 1. Carichiamo il gruppo per sapere chi sono i membri
            val gruppo = gruppoRepository.getGruppoPerId(gruppoId)
            val tuttiAmici = amicoRepository.getTuttiGliAmici()

            if (gruppo != null) {
                // Filtriamo: prendiamo solo gli amici i cui ID sono nel gruppo
                _amiciDelGruppo.value = tuttiAmici.filter { gruppo.membriIds.contains(it.id) }

                // Pre-selezioniamo tutti come partecipanti di default per comodità?
                // Meglio di no, lasciamo scegliere all'utente.
            }

            // 2. Se è una modifica, carichiamo i dati dell'uscita
            if (uscitaId.isNotEmpty()) {
                val uscita = uscitaRepository.getUscitaPerId(uscitaId)
                if (uscita != null) {
                    _nomeUscita.value = uscita.nome
                    _partecipantiSelezionati.value = uscita.partecipantiIds.toSet()
                    _guidatoriSelezionati.value = uscita.guidatoriIds.toSet()
                    _kmTotali.value = uscita.kmTotali
                }
            } else {
                // Reset per nuova uscita
                _nomeUscita.value = ""
                _partecipantiSelezionati.value = emptySet()
                _guidatoriSelezionati.value = emptySet()
                _kmTotali.value = 0
            }
        }
    }

    fun onNomeUscitaChange(nuovoNome: String) {
        _nomeUscita.value = nuovoNome
    }

    fun togglePartecipanteSelezionato(amicoId: String) {
        _partecipantiSelezionati.value = _partecipantiSelezionati.value.toMutableSet().apply {
            if (contains(amicoId)) {
                remove(amicoId)
                // Se rimuovi un partecipante, rimuovilo anche dai guidatori!
                val nuoviGuidatori = _guidatoriSelezionati.value.toMutableSet()
                nuoviGuidatori.remove(amicoId)
                _guidatoriSelezionati.value = nuoviGuidatori
            } else {
                add(amicoId)
            }
        }
    }

    fun toggleGuidatoreSelezionato(amicoId: String) {
        // Puoi essere guidatore solo se sei partecipante
        if (_partecipantiSelezionati.value.contains(amicoId)) {
            _guidatoriSelezionati.value = _guidatoriSelezionati.value.toMutableSet().apply {
                if (contains(amicoId)) remove(amicoId) else add(amicoId)
            }
        }
    }

    fun setKmTotali(km: Int) {
        _kmTotali.value = km
    }

    fun salvaUscita(onSalvato: () -> Unit) {
        viewModelScope.launch {
            val partecipantiIdsList = _partecipantiSelezionati.value.toList()
            val guidatoriIdsList = _guidatoriSelezionati.value.toList()

            // Aggiorniamo anche le statistiche degli amici (algoritmo live!)
            // Nota: Questo è un approccio semplice. In una app complessa si gestisce lato backend.
            // Qui lo facciamo per vedere subito i risultati.
            if (currentUscitaId.isEmpty()) {
                // NUOVA USCITA
                val nuovaUscita = Uscita(
                    id = "", // Generato dal repo
                    nome = _nomeUscita.value,
                    gruppoId = currentGruppoId,
                    partecipantiIds = partecipantiIdsList,
                    kmTotali = _kmTotali.value,
                    guidatoriIds = guidatoriIdsList
                )
                uscitaRepository.aggiungiUscita(nuovaUscita)

                // Aggiorna statistiche amici (Km, Presenze)
                aggiornaStatisticheAmici(partecipantiIdsList, guidatoriIdsList, _kmTotali.value)

            } else {
                // MODIFICA (Attenzione: gestire le statistiche in modifica è complesso,
                // per ora aggiorniamo solo l'uscita senza ricalcolare tutto lo storico per semplicità)
                val uscitaAggiornata = Uscita(
                    id = currentUscitaId,
                    nome = _nomeUscita.value,
                    gruppoId = currentGruppoId,
                    partecipantiIds = partecipantiIdsList,
                    kmTotali = _kmTotali.value,
                    guidatoriIds = guidatoriIdsList
                )
                uscitaRepository.aggiornaUscita(uscitaAggiornata)
            }
            onSalvato()
        }
    }

    private fun aggiornaStatisticheAmici(partecipanti: List<String>, guidatori: List<String>, km: Int) {
        partecipanti.forEach { id ->
            val haGuidato = guidatori.contains(id)
            // Se hanno guidato in più persone, dividiamo i km?
            // Per ora assegniamo i km interi al guidatore principale o a tutti i guidatori (semplificazione)
            val kmGuidatiReali = if (haGuidato) km else 0

            amicoRepository.aggiornaStatisticheAmico(id, kmGuidatiReali, haGuidato)
        }
    }

    fun eliminaUscita(onEliminato: () -> Unit) {
        viewModelScope.launch {
            if (currentUscitaId.isNotEmpty()) {
                uscitaRepository.eliminaUscita(currentUscitaId)
            }
            onEliminato()
        }
    }
}