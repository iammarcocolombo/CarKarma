package it.col.mar.android.carkarma.presentation.gruppo.modifica

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

    // CORREZIONE: Colleghiamo direttamente il flusso del repository.
    // In questo modo, qualsiasi cambiamento nel DB (anche se aggiungi un amico da un'altra parte)
    // si riflette immediatamente qui senza bisogno di init o reload manuali.
    val amiciDisponibili: StateFlow<List<Amico>> = amicoRepository.amici

    // Set degli ID degli amici selezionati
    private val _amiciSelezionati = MutableStateFlow<Set<String>>(emptySet())
    val amiciSelezionati: StateFlow<Set<String>> = _amiciSelezionati

    private var currentGruppoId: String = ""

    fun loadGruppo(gruppoId: String) {
        this.currentGruppoId = gruppoId
        viewModelScope.launch {
            // Se stiamo modificando, carica i dati del gruppo
            if (gruppoId.isNotEmpty()) {
                val gruppo = gruppoRepository.getGruppoPerId(gruppoId)
                if (gruppo != null) {
                    _nomeGruppo.value = gruppo.nome
                    // Importante: In Gruppo ora abbiamo membriIds (List<String>)
                    _amiciSelezionati.value = gruppo.membriIds.toSet()
                }
            } else {
                // Nuovo gruppo: resetta i campi
                _nomeGruppo.value = ""
                _amiciSelezionati.value = emptySet()
            }
        }
    }

    fun onNomeGruppoChange(nuovoNome: String) {
        _nomeGruppo.value = nuovoNome
    }

    fun toggleAmicoSelezionato(amicoId: String) {
        _amiciSelezionati.value = _amiciSelezionati.value.toMutableSet().apply {
            if (contains(amicoId)) remove(amicoId) else add(amicoId)
        }
    }

    fun salvaGruppo(onSalvato: () -> Unit) {
        viewModelScope.launch {
            // Prendiamo gli ID selezionati
            val listaMembriIds = _amiciSelezionati.value.toList()

            if (currentGruppoId.isEmpty()) {
                // NUOVO GRUPPO
                val nuovoGruppo = Gruppo(
                    id = "",
                    nome = _nomeGruppo.value,
                    membriIds = listaMembriIds
                )
                gruppoRepository.aggiungiGruppo(nuovoGruppo)
            } else {
                // MODIFICA ESISTENTE
                val gruppoAggiornato = Gruppo(
                    id = currentGruppoId,
                    nome = _nomeGruppo.value,
                    membriIds = listaMembriIds
                )
                gruppoRepository.aggiornaGruppo(gruppoAggiornato)
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