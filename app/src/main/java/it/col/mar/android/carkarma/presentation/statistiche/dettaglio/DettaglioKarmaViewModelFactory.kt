package it.col.mar.android.carkarma.presentation.statistiche.dettaglio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.col.mar.android.carkarma.domain.repository.GruppoRepository

class DettaglioKarmaViewModelFactory(
    private val gruppoRepository: GruppoRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DettaglioKarmaViewModel::class.java)) {
            return DettaglioKarmaViewModel(gruppoRepository) as T
        }
        throw IllegalArgumentException("Classe ViewModel sconosciuta")
    }
}