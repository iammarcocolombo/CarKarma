package it.col.mar.android.carkarma.presentation.gruppo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.col.mar.android.carkarma.data.database.AmicoRepository
import it.col.mar.android.carkarma.data.database.AppContainer
import it.col.mar.android.carkarma.data.database.GruppoRepository


class ModificaGruppoViewModelFactory(
    private val gruppoRepository: GruppoRepository,
    private val amicoRepository: AmicoRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ModificaGruppoViewModel::class.java)) {
            return ModificaGruppoViewModel(gruppoRepository, amicoRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
