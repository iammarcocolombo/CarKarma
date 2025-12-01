package it.col.mar.android.carkarma.presentation.gruppo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.col.mar.android.carkarma.data.database.AmicoRepository
import it.col.mar.android.carkarma.data.database.GruppoRepository
import it.col.mar.android.carkarma.data.database.UscitaRepository

class GruppoViewModelFactory(
    private val amicoRepository: AmicoRepository,
    private val gruppoRepository: GruppoRepository,
    private val uscitaRepository: UscitaRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GruppoViewModel::class.java)) {
            return GruppoViewModel(
                amicoRepository,
                gruppoRepository,
                uscitaRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}