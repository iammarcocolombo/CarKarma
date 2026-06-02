package it.col.mar.android.carkarma.presentation.gruppo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.col.mar.android.carkarma.domain.repository.GruppoRepository
import it.col.mar.android.carkarma.domain.repository.UscitaRepository

class GruppoViewModelFactory(
    private val gruppoRepository: GruppoRepository,
    private val uscitaRepository: UscitaRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GruppoViewModel::class.java)) {
            return GruppoViewModel(
                gruppoRepository,
                uscitaRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}