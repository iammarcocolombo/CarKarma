package it.col.mar.android.carkarma.presentation.home

import androidx.lifecycle.ViewModel
import it.col.mar.android.carkarma.data.database.GruppoRepository
import it.col.mar.android.carkarma.data.model.Gruppo
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel(
    private val repository: GruppoRepository
) : ViewModel() {

    // Espone direttamente il flow dal repository
    val gruppi: StateFlow<List<Gruppo>> = repository.gruppi

    // Funzione helper per aggiungere (anche se di solito si fa dalla schermata di modifica)
    fun aggiungiGruppo(gruppo: Gruppo) {
        repository.aggiungiGruppo(gruppo)
    }
}