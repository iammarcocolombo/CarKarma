package it.col.mar.android.carkarma.presentation.amico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.col.mar.android.carkarma.domain.repository.AmicoRepository
import it.col.mar.android.carkarma.domain.repository.GruppoRepository

class AmicoViewModelFactory(
    private val amicoRepository: AmicoRepository,
    private val gruppoRepository: GruppoRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AmicoViewModel::class.java)) {
            return AmicoViewModel(amicoRepository, gruppoRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}