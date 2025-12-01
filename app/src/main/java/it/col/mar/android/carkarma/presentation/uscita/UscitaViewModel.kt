package it.col.mar.android.carkarma.presentation.uscita

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.col.mar.android.carkarma.data.database.GruppoRepository
import it.col.mar.android.carkarma.data.database.UscitaRepository
import it.col.mar.android.carkarma.data.model.Amico
import it.col.mar.android.carkarma.data.model.Uscita
import it.col.mar.android.carkarma.domain.CalcoloTurnoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class UscitaViewModel(
    private val uscitaRepository: UscitaRepository,
    private val gruppoRepository: GruppoRepository
) : ViewModel() {

    private val calcoloUseCase = CalcoloTurnoUseCase()

    private var currentUscitaId: String = ""
    private var currentGruppoId: String = ""

    private val _nomeUscita = MutableStateFlow("")
    val nomeUscita: StateFlow<String> = _nomeUscita

    private val _amiciDelGruppo = MutableStateFlow<List<Amico>>(emptyList())
    val amiciDelGruppo: StateFlow<List<Amico>> = _amiciDelGruppo

    private val _partecipantiSelezionati = MutableStateFlow<Set<String>>(emptySet())
    val partecipantiSelezionati: StateFlow<Set<String>> = _partecipantiSelezionati

    private val _guidatoriSelezionati = MutableStateFlow<Set<String>>(emptySet())
    val guidatoriSelezionati: StateFlow<Set<String>> = _guidatoriSelezionati

    private val _kmTotali = MutableStateFlow(0)
    val kmTotali: StateFlow<Int> = _kmTotali

    private val _suggerimentoGuidatore = MutableStateFlow<String?>(null)
    val suggerimentoGuidatore: StateFlow<String?> = _suggerimentoGuidatore

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun loadUscita(gruppoId: String, uscitaId: String) {
        this.currentGruppoId = gruppoId
        this.currentUscitaId = uscitaId

        // 1. Carichiamo i membri dal GRUPPO (sottocollezione)
        // Fondamentale: usiamo i dati "istanza" con i km specifici di questo gruppo
        viewModelScope.launch {
            gruppoRepository.getMembriDelGruppo(gruppoId).collect { membri ->
                _amiciDelGruppo.value = membri
            }
        }

        // 2. Se è una modifica, carichiamo i dati dell'uscita
        if (uscitaId.isNotEmpty()) {
            viewModelScope.launch {
                // CORREZIONE: Ora passiamo anche gruppoId perché le uscite sono annidate
                val uscita = uscitaRepository.getUscita(gruppoId, uscitaId)
                if (uscita != null) {
                    _nomeUscita.value = uscita.nome
                    _partecipantiSelezionati.value = uscita.partecipantiIds.toSet()
                    _guidatoriSelezionati.value = uscita.guidatoriIds.toSet()
                    _kmTotali.value = uscita.kmTotali
                }
            }
        }
    }

    fun onNomeUscitaChange(nuovoNome: String) { _nomeUscita.value = nuovoNome }

    fun togglePartecipanteSelezionato(amicoId: String) {
        _partecipantiSelezionati.value = _partecipantiSelezionati.value.toMutableSet().apply {
            if (contains(amicoId)) {
                remove(amicoId)
                val nuoviGuidatori = _guidatoriSelezionati.value.toMutableSet()
                nuoviGuidatori.remove(amicoId)
                _guidatoriSelezionati.value = nuoviGuidatori
            } else {
                add(amicoId)
            }
        }
    }

    fun toggleGuidatoreSelezionato(amicoId: String) {
        if (_partecipantiSelezionati.value.contains(amicoId)) {
            _guidatoriSelezionati.value = _guidatoriSelezionati.value.toMutableSet().apply {
                if (contains(amicoId)) remove(amicoId) else add(amicoId)
            }
        }
    }

    fun setKmTotali(km: Int) { _kmTotali.value = km }

    // --- ALGORITMO DI SUGGERIMENTO ---
    fun calcolaSuggerimento() {
        val amiciOggetti = _amiciDelGruppo.value
        val partecipantiIds = _partecipantiSelezionati.value

        if (partecipantiIds.size < 2) {
            _suggerimentoGuidatore.value = "Seleziona almeno 2 partecipanti."
            return
        }

        val classifica = calcoloUseCase.calcolaChiGuida(amiciOggetti, partecipantiIds)

        if (classifica.isNotEmpty()) {
            if (classifica.size == 1) {
                val (prescelto, karma) = classifica.first()
                _suggerimentoGuidatore.value = "🚗 Dovrebbe guidare:\n${prescelto.nome}\n(Karma Gruppo: ${String.format(Locale.US, "%.1f", karma)})"
            } else {
                val sb = StringBuilder("🚗 Servono ${classifica.size} auto!\nEcco la squadra ideale:\n\n")
                classifica.forEachIndexed { index, (amico, karma) ->
                    sb.append("${index + 1}. ${amico.nome} (${amico.postiAuto} posti)\n   Karma Gruppo: ${String.format(Locale.US, "%.1f", karma)}\n")
                }

                val postiTotali = classifica.sumOf { it.first.postiAuto }
                if (postiTotali < partecipantiIds.size) {
                    sb.append("\n⚠️ ATTENZIONE: Mancano ancora ${partecipantiIds.size - postiTotali} posti!")
                }
                _suggerimentoGuidatore.value = sb.toString()
            }
        } else {
            _suggerimentoGuidatore.value = "Nessuna soluzione trovata! Controlla i posti auto."
        }
    }

    fun resetSuggerimento() { _suggerimentoGuidatore.value = null }
    fun clearError() { _errorMessage.value = null }

    // --- SALVATAGGIO ---
    fun salvaUscita(onSalvato: () -> Unit) {
        if (_partecipantiSelezionati.value.size < 2) {
            _errorMessage.value = "Un'uscita richiede almeno 2 partecipanti."
            return
        }

        viewModelScope.launch {
            val partecipantiList = _partecipantiSelezionati.value.toList()
            val guidatoriList = _guidatoriSelezionati.value.toList()

            // Creiamo l'oggetto Uscita
            // L'ID viene gestito dentro aggiungiUscita se vuoto, ma qui possiamo lasciarlo vuoto o gestirlo
            // Per coerenza con il repo, passiamo l'oggetto.
            val uscita = Uscita(
                id = if (currentUscitaId.isEmpty()) "" else currentUscitaId,
                nome = _nomeUscita.value,
                gruppoId = currentGruppoId,
                partecipantiIds = partecipantiList,
                kmTotali = _kmTotali.value,
                guidatoriIds = guidatoriList
            )

            if (currentUscitaId.isEmpty()) {
                // NUOVA USCITA
                uscitaRepository.aggiungiUscita(uscita)

                // Aggiorniamo statistiche SOLO se è nuova uscita (per non duplicare i km in modifica)
                aggiornaStatisticheMembriGruppo(partecipantiList, guidatoriList, _kmTotali.value)
            } else {
                // MODIFICA
                uscitaRepository.aggiornaUscita(uscita)
                // Nota: In un'app completa qui dovremmo fare il rollback delle statistiche vecchie
                // e applicare le nuove. Per semplicità in questa versione non lo facciamo.
            }
            onSalvato()
        }
    }

    private fun aggiornaStatisticheMembriGruppo(partecipanti: List<String>, guidatori: List<String>, km: Int) {
        partecipanti.forEach { id ->
            val haGuidato = guidatori.contains(id)
            val kmGuidatiReali = if (haGuidato) km else 0

            // Usiamo il metodo del GruppoRepository per aggiornare la sottocollezione
            gruppoRepository.aggiornaStatisticheMembro(currentGruppoId, id, kmGuidatiReali, haGuidato)
        }
    }

    fun eliminaUscita(onEliminato: () -> Unit) {
        viewModelScope.launch {
            if (currentUscitaId.isNotEmpty()) {
                // CORREZIONE: Passiamo anche il gruppoId per trovare l'uscita nella sottocollezione
                uscitaRepository.eliminaUscita(currentGruppoId, currentUscitaId)
            }
            onEliminato()
        }
    }
}