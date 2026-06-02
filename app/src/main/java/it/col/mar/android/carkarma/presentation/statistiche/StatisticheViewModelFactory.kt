package it.col.mar.android.carkarma.presentation.statistiche

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.col.mar.android.carkarma.domain.repository.GruppoRepository // CORRETTO: Importiamo l'interfaccia di dominio
import it.col.mar.android.carkarma.domain.repository.UscitaRepository // CORRETTO: Importiamo l'interfaccia di dominio

class StatisticheViewModelFactory(
    private val gruppoRepository: GruppoRepository, // CORRETTO: Tipo cambiato a Interfaccia
    private val uscitaRepository: UscitaRepository  // CORRETTO: Tipo cambiato a Interfaccia
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatisticheViewModel::class.java)) {
            return StatisticheViewModel(gruppoRepository, uscitaRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}