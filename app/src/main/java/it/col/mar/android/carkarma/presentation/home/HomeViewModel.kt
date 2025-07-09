package it.col.mar.android.carkarma.presentation.home

import androidx.lifecycle.ViewModel
import it.col.mar.android.carkarma.data.database.GruppoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import it.col.mar.android.carkarma.data.model.Gruppo

class HomeViewModel : ViewModel() {

    private val repository = GruppoRepository()

    private val _gruppi = MutableStateFlow<List<Gruppo>>(emptyList())
    val gruppi: StateFlow<List<Gruppo>> = _gruppi

    init {
        _gruppi.value = repository.getTuttiIGruppi()
    }

    fun aggiungiGruppo(gruppo: Gruppo) {
        repository.aggiungiGruppo(gruppo)
        _gruppi.value = repository.getTuttiIGruppi()
    }
}