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

    fun loadGruppo(gruppoId: String) {
        viewModelScope.launch {
            if (gruppoId.isEmpty()) {
                _errorMessage.value = "ID gruppo non valido"
                _gruppo.value = null
                _uscite.value = emptyList()
                return@launch
            }

            // Usiamo i metodi aggiornati che accettano String
            val g = gruppoRepository.getGruppoPerId(gruppoId)

            if (g == null) {
                _errorMessage.value = "Gruppo non trovato"
                _gruppo.value = null
                _uscite.value = emptyList()
            } else {
                _errorMessage.value = null
                _gruppo.value = g
                // Carichiamo anche le uscite del gruppo
                _uscite.value = uscitaRepository.getUscitePerGruppo(gruppoId)
            }
        }
    }
}