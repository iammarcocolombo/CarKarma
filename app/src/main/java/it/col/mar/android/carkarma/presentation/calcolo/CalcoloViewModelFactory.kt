package it.col.mar.android.carkarma.presentation.calcolo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.col.mar.android.carkarma.data.database.AmicoRepository
import it.col.mar.android.carkarma.data.database.GruppoRepository

class CalcoloViewModelFactory(
    private val gruppoRepository: GruppoRepository,
    private val amicoRepository: AmicoRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalcoloViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalcoloViewModel(gruppoRepository, amicoRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}