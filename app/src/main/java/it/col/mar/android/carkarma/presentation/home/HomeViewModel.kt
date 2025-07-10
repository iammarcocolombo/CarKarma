package it.col.mar.android.carkarma.presentation.home

import androidx.lifecycle.ViewModel
import it.col.mar.android.carkarma.data.database.AmicoRepository
import it.col.mar.android.carkarma.data.database.GruppoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import it.col.mar.android.carkarma.data.model.Gruppo

class HomeViewModel(
    private val repository: GruppoRepository
) : ViewModel() {

    // Qui direttamente il flow dal repository
    val gruppi: StateFlow<List<Gruppo>> = repository.gruppi

    fun aggiungiGruppo(gruppo: Gruppo) {
        repository.aggiungiGruppo(gruppo)
    }
}
