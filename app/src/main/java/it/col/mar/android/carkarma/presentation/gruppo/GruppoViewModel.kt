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
    private val amicoRepository: AmicoRepository = AmicoRepository(),
    private val gruppoRepository: GruppoRepository = GruppoRepository(amicoRepository),
    private val uscitaRepository: UscitaRepository = UscitaRepository()
) : ViewModel() {

    private val _gruppo = MutableStateFlow<Gruppo?>(null)
    val gruppo: StateFlow<Gruppo?> = _gruppo

    private val _uscite = MutableStateFlow<List<Uscita>>(emptyList())
    val uscite: StateFlow<List<Uscita>> = _uscite

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun loadGruppo(gruppoId: Int) {
        viewModelScope.launch {
            if (gruppoId == -1) {
                _errorMessage.value = "ID gruppo non valido"
                _gruppo.value = null
                _uscite.value = emptyList()
                return@launch
            }
            val g = gruppoRepository.getGruppoPerId(gruppoId)
            if (g == null) {
                _errorMessage.value = "Gruppo non trovato"
                _gruppo.value = null
                _uscite.value = emptyList()
            } else {
                _errorMessage.value = null
                _gruppo.value = g
                _uscite.value = uscitaRepository.getUscitePerGruppo(gruppoId)
            }
        }
    }
}
