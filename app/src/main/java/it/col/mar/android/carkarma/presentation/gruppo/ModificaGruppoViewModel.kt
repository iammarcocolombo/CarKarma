package it.col.mar.android.carkarma.presentation.gruppo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.col.mar.android.carkarma.data.database.AmicoRepository
import it.col.mar.android.carkarma.data.database.GruppoRepository
import it.col.mar.android.carkarma.data.model.Amico
import it.col.mar.android.carkarma.data.model.Gruppo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ModificaGruppoViewModel(
    private val gruppoRepository: GruppoRepository,
    private val amicoRepository: AmicoRepository
) : ViewModel() {

    private val _nomeGruppo = MutableStateFlow("")
    val nomeGruppo: StateFlow<String> = _nomeGruppo

    private val _amiciDisponibili = MutableStateFlow<List<Amico>>(emptyList())
    val amiciDisponibili: StateFlow<List<Amico>> = _amiciDisponibili

    private val _amiciSelezionati = MutableStateFlow<Set<Int>>(emptySet())
    val amiciSelezionati: StateFlow<Set<Int>> = _amiciSelezionati

    private var gruppoId: Int = -1

    fun loadGruppo(gruppoId: Int) {
        this.gruppoId = gruppoId
        viewModelScope.launch {
            // carica tutti gli amici disponibili
            _amiciDisponibili.value = amicoRepository.getTuttiGliAmici()

            if (gruppoId != -1) {
                val gruppo = gruppoRepository.getGruppoPerId(gruppoId)
                if (gruppo != null) {
                    _nomeGruppo.value = gruppo.nome
                    _amiciSelezionati.value = gruppo.amici.map { it.id }.toSet()
                }
            }
        }
    }

    fun onNomeGruppoChange(nuovoNome: String) {
        _nomeGruppo.value = nuovoNome
    }

    fun toggleAmicoSelezionato(amicoId: Int) {
        _amiciSelezionati.value = _amiciSelezionati.value.toMutableSet().apply {
            if (contains(amicoId)) remove(amicoId) else add(amicoId)
        }
    }

    fun salvaGruppo(onSalvato: () -> Unit) {
        viewModelScope.launch {
            val amiciSelezionatiList = _amiciDisponibili.value.filter { _amiciSelezionati.value.contains(it.id) }
            if (gruppoId == -1) {
                // nuovo gruppo
                val nuovoGruppo = Gruppo(
                    id = gruppoRepository.generaNuovoId(),
                    nome = _nomeGruppo.value,
                    amici = amiciSelezionatiList
                )
                gruppoRepository.aggiungiGruppo(nuovoGruppo)
            } else {
                // aggiorna esistente
                val gruppoAggiornato = Gruppo(
                    id = gruppoId,
                    nome = _nomeGruppo.value,
                    amici = amiciSelezionatiList
                )
                gruppoRepository.aggiornaGruppo(gruppoAggiornato)
            }
            onSalvato()
        }
    }
    fun eliminaGruppo(onEliminato: () -> Unit) {
        viewModelScope.launch {
            if (gruppoId != -1) {
                gruppoRepository.eliminaGruppo(gruppoId)
            }
            onEliminato()
        }
    }

}
