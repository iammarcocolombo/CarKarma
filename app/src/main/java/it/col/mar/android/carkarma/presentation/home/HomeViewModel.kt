package it.col.mar.android.carkarma.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.col.mar.android.carkarma.data.database.GruppoRepository
import it.col.mar.android.carkarma.data.model.Gruppo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: GruppoRepository
) : ViewModel() {

    val gruppi: StateFlow<List<Gruppo>> = repository.gruppi

    private val _joinState = MutableStateFlow<JoinState>(JoinState.Idle)
    val joinState: StateFlow<JoinState> = _joinState

    init {
        // SINCRONIZZAZIONE AUTOMATICA ALL'AVVIO
        // Ascoltiamo i gruppi: appena vengono caricati da Firebase,
        // scarichiamo anche i membri e li salviamo nella rubrica personale.
        // Questo garantisce che se ti unisci a un gruppo (anche da un altro dispositivo),
        // ti ritrovi gli amici pronti per creare nuovi gruppi.
        viewModelScope.launch {
            repository.gruppi.collect { listaGruppi ->
                if (listaGruppi.isNotEmpty()) {
                    repository.sincronizzaMembriInRubrica(listaGruppi)
                }
            }
        }
    }

    fun aggiungiGruppo(gruppo: Gruppo) {
        repository.aggiungiGruppo(gruppo)
    }

    fun uniscitiAlGruppo(codiceGruppo: String) {
        if (codiceGruppo.isBlank()) return

        _joinState.value = JoinState.Loading
        viewModelScope.launch {
            repository.uniscitiAlGruppo(codiceGruppo) { successo ->
                if (successo) {
                    _joinState.value = JoinState.Success
                    // Nota: Non serve fare altro qui.
                    // Poiché ci siamo uniti con successo, Firebase aggiornerà la lista 'gruppi' (grazie al listener nel Repository).
                    // Questo farà scattare il 'collect' nel blocco init qui sopra,
                    // che a sua volta chiamerà 'sincronizzaMembriInRubrica'. Tutto automatico!
                } else {
                    _joinState.value = JoinState.Error("Codice non valido o errore di connessione")
                }
            }
        }
    }

    fun resetJoinState() {
        _joinState.value = JoinState.Idle
    }
}

sealed class JoinState {
    data object Idle : JoinState()
    data object Loading : JoinState()
    data object Success : JoinState()
    data class Error(val message: String) : JoinState()
}