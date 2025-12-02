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