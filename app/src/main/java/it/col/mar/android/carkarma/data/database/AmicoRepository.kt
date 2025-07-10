package it.col.mar.android.carkarma.data.database

import it.col.mar.android.carkarma.data.model.Amico
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AmicoRepository {

    private val _amici = MutableStateFlow<List<Amico>>(emptyList())
    val amici: StateFlow<List<Amico>> = _amici.asStateFlow()

    init {
        _amici.value = listOf(
            Amico(1, "Marco", 2, 5, 3, 300),
            Amico(2, "Luca", 1, 3, 2, 200),
            Amico(3, "Giulia", 0, 2, 1, 150)
        )
    }

    fun getTuttiGliAmici(): List<Amico> = _amici.value

    fun aggiungiAmico(amico: Amico) {
        _amici.value = _amici.value + amico
    }

    fun rimuoviAmico(amico: Amico) {
        _amici.value = _amici.value - amico
    }

    fun getAmicoPerId(id: Int): Amico? {
        return _amici.value.find { it.id == id }
    }
}
