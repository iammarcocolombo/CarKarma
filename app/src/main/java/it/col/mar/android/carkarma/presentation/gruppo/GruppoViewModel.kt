package it.col.mar.android.carkarma.presentation.gruppo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.col.mar.android.carkarma.domain.repository.GruppoRepository
import it.col.mar.android.carkarma.domain.repository.UscitaRepository
import it.col.mar.android.carkarma.data.model.Gruppo
import it.col.mar.android.carkarma.data.model.Uscita
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GruppoViewModel(
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

            val g = gruppoRepository.getGruppoPerId(gruppoId)
            if (g == null) {
                _errorMessage.value = "Gruppo non trovato"
                _gruppo.value = null
            } else {
                _errorMessage.value = null
                _gruppo.value = g

                uscitaRepository.getUsciteDelGruppo(gruppoId).collect { listaUscite ->
                    _uscite.value = listaUscite
                }
            }
        }
    }

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