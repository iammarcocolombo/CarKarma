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

    fun uniscitiAlGruppo(inputUtente: String) {
        if (inputUtente.isBlank()) return

        // PULIZIA INTELLIGENTE DELL'INPUT:
        // 1. Rimuove spazi vuoti prima e dopo (.trim())
        // 2. Se l'utente ha incollato un link intero, prendiamo solo l'ultima parte (l'ID)
        var codicePulito = inputUtente.trim()

        if (codicePulito.contains("/")) {
            // Prende tutto quello che c'è dopo l'ultimo slash (es. da "carkarma://join/XYZ" prende "XYZ")
            codicePulito = codicePulito.substringAfterLast("/")
        }

        _joinState.value = JoinState.Loading
        viewModelScope.launch {
            repository.uniscitiAlGruppo(codicePulito) { successo ->
                if (successo) {
                    _joinState.value = JoinState.Success
                } else {
                    _joinState.value = JoinState.Error("Gruppo non trovato o errore di connessione.")
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