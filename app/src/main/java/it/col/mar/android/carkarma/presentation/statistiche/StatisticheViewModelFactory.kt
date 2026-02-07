package it.col.mar.android.carkarma.presentation.statistiche

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.col.mar.android.carkarma.data.database.GruppoRepository
import it.col.mar.android.carkarma.data.database.UscitaRepository

class StatisticheViewModelFactory(
    private val gruppoRepository: GruppoRepository,
    private val uscitaRepository: UscitaRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatisticheViewModel::class.java)) {
            return StatisticheViewModel(gruppoRepository, uscitaRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}