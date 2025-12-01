package it.col.mar.android.carkarma.presentation.uscita

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.col.mar.android.carkarma.data.database.AmicoRepository
import it.col.mar.android.carkarma.data.database.GruppoRepository
import it.col.mar.android.carkarma.data.database.UscitaRepository

class UscitaViewModelFactory(
    private val uscitaRepository: UscitaRepository,
    private val amicoRepository: AmicoRepository,
    private val gruppoRepository: GruppoRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UscitaViewModel::class.java)) {
            return UscitaViewModel(
                uscitaRepository,
                amicoRepository,
                gruppoRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}