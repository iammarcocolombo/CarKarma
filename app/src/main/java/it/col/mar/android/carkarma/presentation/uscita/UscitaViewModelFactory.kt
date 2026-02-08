package it.col.mar.android.carkarma.presentation.uscita

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.col.mar.android.carkarma.data.database.CarburanteRepository
import it.col.mar.android.carkarma.data.database.GruppoRepository
import it.col.mar.android.carkarma.data.database.UscitaRepository

class UscitaViewModelFactory(
    private val uscitaRepository: UscitaRepository,
    private val gruppoRepository: GruppoRepository,
    private val carburanteRepository: CarburanteRepository // <--- 1. NUOVO PARAMETRO
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UscitaViewModel::class.java)) {
            return UscitaViewModel(
                uscitaRepository,
                gruppoRepository,
                carburanteRepository // <--- 2. LO PASSIAMO AL VIEWMODEL
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}