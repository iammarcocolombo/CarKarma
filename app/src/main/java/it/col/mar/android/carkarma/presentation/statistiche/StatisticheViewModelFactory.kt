package it.col.mar.android.carkarma.presentation.statistiche

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.col.mar.android.carkarma.domain.repository.GruppoRepository
import it.col.mar.android.carkarma.domain.repository.UscitaRepository

class StatisticheViewModelFactory(
    private val gruppoRepository: GruppoRepository,
    private val uscitaRepository: UscitaRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatisticheViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatisticheViewModel(
                gruppoRepository = gruppoRepository,
                uscitaRepository = uscitaRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}