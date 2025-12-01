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
    // NOTA: AmicoRepository rimosso perché ora usiamo i dati isolati del Gruppo
) : ViewModel() {

    private val calcoloUseCase = CalcoloTurnoUseCase()

    private var currentUscitaId: String = ""
    private var currentGruppoId: String = ""

    private val _nomeUscita = MutableStateFlow("")
    val nomeUscita: StateFlow<String> = _nomeUscita

    // Questa lista ora contiene le "Istanze" degli amici specifiche per questo gruppo (con i km giusti)
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

        viewModelScope.launch {
            // 1. Carichiamo i membri dalla sottocollezione del GRUPPO.
            // Questi dati sono isolati: i km che vedi qui valgono solo per questo gruppo.
            gruppoRepository.getMembriDelGruppo(gruppoId).collect { membri ->
                _amiciDelGruppo.value = membri
            }
        }

        viewModelScope.launch {
            // 2. Se stiamo modificando un'uscita esistente, carichiamo i suoi dati
            if (uscitaId.isNotEmpty()) {
                val uscita = uscitaRepository.getUscitaPerId(uscitaId)
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
                // Se togli un partecipante, non può essere guidatore
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

        // Passiamo i dati all'algoritmo (che ora supporta N macchine)
        val classifica = calcoloUseCase.calcolaChiGuida(amiciOggetti, partecipantiIds)

        if (classifica.isNotEmpty()) {
            if (classifica.size == 1) {
                // Caso standard: 1 macchina
                val (prescelto, karma) = classifica.first()
                _suggerimentoGuidatore.value = "🚗 Dovrebbe guidare:\n${prescelto.nome}\n(Karma Gruppo: ${String.format(Locale.US, "%.1f", karma)})"
            } else {
                // Caso gruppo numeroso: N macchine
                val sb = StringBuilder("🚗 Servono ${classifica.size} auto!\nEcco la squadra ideale:\n\n")
                classifica.forEachIndexed { index, (amico, karma) ->
                    sb.append("${index + 1}. ${amico.nome} (${amico.postiAuto} posti)\n   Karma Gruppo: ${String.format(Locale.US, "%.1f", karma)}\n")
                }

                // Verifica extra sui posti totali
                val postiTotali = classifica.sumOf { it.first.postiAuto }
                if (postiTotali < partecipantiIds.size) {
                    sb.append("\n⚠️ ATTENZIONE: Mancano ancora ${partecipantiIds.size - postiTotali} posti!")
                }

                _suggerimentoGuidatore.value = sb.toString()
            }
        } else {
            _suggerimentoGuidatore.value = "Nessuna soluzione trovata! Controlla i posti auto disponibili."
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

            if (currentUscitaId.isEmpty()) {
                val nuovaUscita = Uscita(
                    id = "",
                    nome = _nomeUscita.value,
                    gruppoId = currentGruppoId,
                    partecipantiIds = partecipantiList,
                    kmTotali = _kmTotali.value,
                    guidatoriIds = guidatoriList
                )
                uscitaRepository.aggiungiUscita(nuovaUscita)

                // Aggiorniamo le statistiche "istanza" dentro il gruppo
                aggiornaStatisticheMembriGruppo(partecipantiList, guidatoriList, _kmTotali.value)

            } else {
                val uscitaAggiornata = Uscita(
                    id = currentUscitaId,
                    nome = _nomeUscita.value,
                    gruppoId = currentGruppoId,
                    partecipantiIds = partecipantiList,
                    kmTotali = _kmTotali.value,
                    guidatoriIds = guidatoriList
                )
                uscitaRepository.aggiornaUscita(uscitaAggiornata)
            }
            onSalvato()
        }
    }

    // Aggiorna SOLO i membri di questo specifico gruppo
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
                uscitaRepository.eliminaUscita(currentUscitaId)
            }
            onEliminato()
        }
    }
}