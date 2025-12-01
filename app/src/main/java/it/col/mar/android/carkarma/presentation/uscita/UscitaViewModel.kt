package it.col.mar.android.carkarma.presentation.uscita

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.col.mar.android.carkarma.data.database.AmicoRepository
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
    private val amicoRepository: AmicoRepository,
    private val gruppoRepository: GruppoRepository
) : ViewModel() {

    // Istanza dell'algoritmo (logica pura)
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

    // NUOVO STATO: Contiene il messaggio del suggerimento (es. "Tocca a Marco (Karma -50)")
    private val _suggerimentoGuidatore = MutableStateFlow<String?>(null)
    val suggerimentoGuidatore: StateFlow<String?> = _suggerimentoGuidatore

    fun loadUscita(gruppoId: String, uscitaId: String) {
        this.currentGruppoId = gruppoId
        this.currentUscitaId = uscitaId

        viewModelScope.launch {
            val gruppo = gruppoRepository.getGruppoPerId(gruppoId)
            val tuttiAmici = amicoRepository.getTuttiGliAmici()

            if (gruppo != null) {
                _amiciDelGruppo.value = tuttiAmici.filter { gruppo.membriIds.contains(it.id) }
            }

            if (uscitaId.isNotEmpty()) {
                val uscita = uscitaRepository.getUscitaPerId(uscitaId)
                if (uscita != null) {
                    _nomeUscita.value = uscita.nome
                    _partecipantiSelezionati.value = uscita.partecipantiIds.toSet()
                    _guidatoriSelezionati.value = uscita.guidatoriIds.toSet()
                    _kmTotali.value = uscita.kmTotali
                }
            } else {
                _nomeUscita.value = ""
                _partecipantiSelezionati.value = emptySet()
                _guidatoriSelezionati.value = emptySet()
                _kmTotali.value = 0
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

    // NUOVA FUNZIONE: Esegue l'algoritmo sui partecipanti attuali
    fun calcolaSuggerimento() {
        val amiciOggetti = _amiciDelGruppo.value
        val partecipantiIds = _partecipantiSelezionati.value

        if (partecipantiIds.isEmpty()) {
            _suggerimentoGuidatore.value = "Seleziona almeno un partecipante!"
            return
        }

        // Chiamiamo l'algoritmo che hai nel domain layer
        val classifica = calcoloUseCase.calcolaChiGuida(amiciOggetti, partecipantiIds)

        if (classifica.isNotEmpty()) {
            val (prescelto, karma) = classifica.first()
            _suggerimentoGuidatore.value = "Dovrebbe guidare: ${prescelto.nome}\n(Karma: ${String.format(Locale.US, "%.1f", karma)})"
        } else {
            _suggerimentoGuidatore.value = "Nessun dato disponibile per il calcolo."
        }
    }

    // Funzione per chiudere il dialog resettando lo stato
    fun resetSuggerimento() {
        _suggerimentoGuidatore.value = null
    }

    fun salvaUscita(onSalvato: () -> Unit) {
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
                aggiornaStatisticheAmici(partecipantiList, guidatoriList, _kmTotali.value)
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

    private fun aggiornaStatisticheAmici(partecipanti: List<String>, guidatori: List<String>, km: Int) {
        partecipanti.forEach { id ->
            val haGuidato = guidatori.contains(id)
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