package it.col.mar.android.carkarma.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.col.mar.android.carkarma.data.model.Gruppo
import it.col.mar.android.carkarma.domain.repository.GruppoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: GruppoRepository
) : ViewModel() {

    val gruppi: StateFlow<List<Gruppo>> = repository.gruppi

    private val _joinState = MutableStateFlow<JoinState>(JoinState.Idle)
    val joinState: StateFlow<JoinState> = _joinState

    init {
        // CORREZIONE FONDAMENTALE: Usiamo first() filtrando la prima lista valida.
        // In questo modo esegue la sincronizzazione una sola volta all'avvio e killa la sottoscrizione.
        viewModelScope.launch {
            val primaListaValida = repository.gruppi.first { it.isNotEmpty() }
            repository.sincronizzaMembriInRubrica(primaListaValida)
        }
    }

    fun uniscitiAlGruppo(inputUtente: String) {
        if (inputUtente.isBlank()) return

        var codicePulito = inputUtente.trim()
        if (codicePulito.contains("/")) {
            codicePulito = codicePulito.substringAfterLast("/")
        }

        _joinState.value = JoinState.Loading
        viewModelScope.launch {
            repository.uniscitiAlGruppo(codicePulito) { successo ->
                _joinState.value = if (successo) {
                    JoinState.Success
                } else {
                    JoinState.Error("Gruppo non trovato o errore di connessione.")
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