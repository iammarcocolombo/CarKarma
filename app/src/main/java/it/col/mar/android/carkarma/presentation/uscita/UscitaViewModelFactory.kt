package it.col.mar.android.carkarma.presentation.uscita

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.col.mar.android.carkarma.domain.repository.CarburanteRepository
import it.col.mar.android.carkarma.domain.repository.GruppoRepository
import it.col.mar.android.carkarma.domain.repository.UscitaRepository

class UscitaViewModelFactory(
    private val uscitaRepository: UscitaRepository,
    private val gruppoRepository: GruppoRepository,
    private val carburanteRepository: CarburanteRepository // FIX: Reinserita l'interfaccia nel costruttore
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UscitaViewModel::class.java)) {
            return UscitaViewModel(
                uscitaRepository = uscitaRepository,
                gruppoRepository = gruppoRepository,
                carburanteRepository = carburanteRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}