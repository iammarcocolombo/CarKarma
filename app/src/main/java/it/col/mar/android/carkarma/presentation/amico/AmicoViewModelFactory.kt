package it.col.mar.android.carkarma.presentation.amico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.col.mar.android.carkarma.data.database.AmicoRepository

class AmicoViewModelFactory(
    private val amicoRepository: AmicoRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AmicoViewModel::class.java)) {
            return AmicoViewModel(amicoRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}