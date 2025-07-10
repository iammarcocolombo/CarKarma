package it.col.mar.android.carkarma.presentation.uscita

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.col.mar.android.carkarma.data.database.AmicoRepository
import it.col.mar.android.carkarma.data.database.UscitaRepository
import it.col.mar.android.carkarma.data.model.Amico
import it.col.mar.android.carkarma.data.model.Uscita
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UscitaViewModel(
    private val uscitaRepository: UscitaRepository,
    private val amicoRepository: AmicoRepository
) : ViewModel() {

    private var uscitaId: Int = -1
    private var gruppoId: Int = -1

    private val _nomeUscita = MutableStateFlow("")
    val nomeUscita: StateFlow<String> = _nomeUscita

    private val _amiciDisponibili = MutableStateFlow<List<Amico>>(emptyList())
    val amiciDisponibili: StateFlow<List<Amico>> = _amiciDisponibili

    private val _partecipantiSelezionati = MutableStateFlow<Set<Int>>(emptySet())
    val partecipantiSelezionati: StateFlow<Set<Int>> = _partecipantiSelezionati

    private val _guidatoriSelezionati = MutableStateFlow<Set<Int>>(emptySet())
    val guidatoriSelezionati: StateFlow<Set<Int>> = _guidatoriSelezionati

    private val _kmTotali = MutableStateFlow(0)
    val kmTotali: StateFlow<Int> = _kmTotali

    fun loadUscita(gruppoId: Int, uscitaId: Int) {
        this.gruppoId = gruppoId
        this.uscitaId = uscitaId

        viewModelScope.launch {
            _amiciDisponibili.value = amicoRepository.getTuttiGliAmici()

            if (uscitaId != -1) {
                val uscita = uscitaRepository.getUscitaPerId(uscitaId)
                if (uscita != null) {
                    _nomeUscita.value = uscita.nome
                    _partecipantiSelezionati.value = uscita.partecipanti.map { it.id }.toSet()
                    _guidatoriSelezionati.value = uscita.guidatori.map { it.id }.toSet()
                    _kmTotali.value = uscita.kmTotali
                }
            }
        }
    }

    fun onNomeUscitaChange(nuovoNome: String) {
        _nomeUscita.value = nuovoNome
    }

    fun togglePartecipanteSelezionato(amicoId: Int) {
        _partecipantiSelezionati.value = _partecipantiSelezionati.value.toMutableSet().apply {
            if (contains(amicoId)) remove(amicoId) else add(amicoId)
        }
    }

    fun toggleGuidatoreSelezionato(amicoId: Int) {
        _guidatoriSelezionati.value = _guidatoriSelezionati.value.toMutableSet().apply {
            if (contains(amicoId)) remove(amicoId) else add(amicoId)
        }
    }

    fun setKmTotali(km: Int) {
        _kmTotali.value = km
    }

    fun salvaUscita(onSalvato: () -> Unit) {
        viewModelScope.launch {
            val partecipantiList = _amiciDisponibili.value.filter { _partecipantiSelezionati.value.contains(it.id) }
            val guidatoriList = _amiciDisponibili.value.filter { _guidatoriSelezionati.value.contains(it.id) }

            if (uscitaId == -1) {
                // Nuova uscita
                val nuovaUscita = Uscita(
                    id = uscitaRepository.generaNuovoId(),
                    nome = _nomeUscita.value,
                    gruppoId = gruppoId,
                    partecipanti = partecipantiList,
                    kmTotali = _kmTotali.value,
                    guidatori = guidatoriList
                )
                uscitaRepository.aggiungiUscita(nuovaUscita)
            } else {
                // Modifica uscita esistente
                val uscitaAggiornata = Uscita(
                    id = uscitaId,
                    nome = _nomeUscita.value,
                    gruppoId = gruppoId,
                    partecipanti = partecipantiList,
                    kmTotali = _kmTotali.value,
                    guidatori = guidatoriList
                )
                uscitaRepository.aggiornaUscita(uscitaAggiornata)
            }
            onSalvato()
        }
    }
    fun eliminaUscita(onEliminato: () -> Unit) {
        viewModelScope.launch {
            if (uscitaId != -1) {
                uscitaRepository.eliminaUscita(uscitaId)
            }
            onEliminato()
        }
    }

}
