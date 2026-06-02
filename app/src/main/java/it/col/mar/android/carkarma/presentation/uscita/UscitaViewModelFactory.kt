package it.col.mar.android.carkarma.presentation.uscita

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.col.mar.android.carkarma.data.database.AppContainer.carburanteRepository
import it.col.mar.android.carkarma.domain.repository.GruppoRepository
import it.col.mar.android.carkarma.domain.repository.UscitaRepository

class UscitaViewModelFactory(
    private val uscitaRepository: UscitaRepository,
    private val gruppoRepository: GruppoRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UscitaViewModel::class.java)) {
            return UscitaViewModel(
                uscitaRepository,
                gruppoRepository,
                carburanteRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}