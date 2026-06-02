package it.col.mar.android.carkarma.presentation.gruppo.modifica

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.col.mar.android.carkarma.domain.repository.AmicoRepository
import it.col.mar.android.carkarma.domain.repository.GruppoRepository

class ModificaGruppoViewModelFactory(
    private val gruppoRepository: GruppoRepository,
    private val amicoRepository: AmicoRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ModificaGruppoViewModel::class.java)) {
            return ModificaGruppoViewModel(gruppoRepository, amicoRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}