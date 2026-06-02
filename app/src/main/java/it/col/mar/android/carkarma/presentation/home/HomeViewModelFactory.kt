package it.col.mar.android.carkarma.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.col.mar.android.carkarma.domain.repository.GruppoRepository // CORRETTO: Dipende dall'interfaccia di dominio

class HomeViewModelFactory(
    private val repository: GruppoRepository // CORRETTO: Parametro aggiornato all'interfaccia astratta
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}