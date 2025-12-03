package it.col.mar.android.carkarma.presentation.gruppo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.col.mar.android.carkarma.data.database.AmicoRepository
import it.col.mar.android.carkarma.data.database.GruppoRepository
import it.col.mar.android.carkarma.data.database.UscitaRepository
import it.col.mar.android.carkarma.data.model.Gruppo
import it.col.mar.android.carkarma.data.model.Uscita
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GruppoViewModel(
    private val amicoRepository: AmicoRepository,
    private val gruppoRepository: GruppoRepository,
    private val uscitaRepository: UscitaRepository
) : ViewModel() {

    private val _gruppo = MutableStateFlow<Gruppo?>(null)
    val gruppo: StateFlow<Gruppo?> = _gruppo

    private val _uscite = MutableStateFlow<List<Uscita>>(emptyList())
    val uscite: StateFlow<List<Uscita>> = _uscite

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private var currentGruppoId: String = ""

    fun loadGruppo(gruppoId: String) {
        this.currentGruppoId = gruppoId
        viewModelScope.launch {
            if (gruppoId.isEmpty()) {
                _errorMessage.value = "ID gruppo non valido"
                return@launch
            }

            // 1. Carica i dettagli del Gruppo
            val g = gruppoRepository.getGruppoPerId(gruppoId)
            if (g == null) {
                _errorMessage.value = "Gruppo non trovato"
                _gruppo.value = null
            } else {
                _errorMessage.value = null
                _gruppo.value = g

                // 2. Ascolta le uscite di questo gruppo in tempo reale
                // (Nota: usa getUsciteDelGruppo che abbiamo aggiunto nel UscitaRepository)
                uscitaRepository.getUsciteDelGruppo(gruppoId).collect { listaUscite ->
                    _uscite.value = listaUscite
                }
            }
        }
    }

    // Funzione per dissociarsi dal gruppo (rimuove il proprio ID dalla lista utenti)
    fun lasciaGruppo(onLasciato: () -> Unit) {
        if (currentGruppoId.isNotEmpty()) {
            viewModelScope.launch {
                gruppoRepository.lasciaGruppo(currentGruppoId) { successo ->
                    if (successo) {
                        onLasciato()
                    } else {
                        _errorMessage.value = "Impossibile lasciare il gruppo. Riprova."
                    }
                }
            }
        }
    }
}