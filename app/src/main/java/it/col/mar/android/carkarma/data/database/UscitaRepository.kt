package it.col.mar.android.carkarma.data.database

import it.col.mar.android.carkarma.data.model.Amico
import it.col.mar.android.carkarma.data.model.Uscita
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UscitaRepository {

    private val _uscite = MutableStateFlow<List<Uscita>>(emptyList())
    val uscite: StateFlow<List<Uscita>> = _uscite.asStateFlow()

    init {
        val amico1 = Amico(1, "Marco", 2, 5, 3, 300)
        val amico2 = Amico(2, "Luca", 1, 3, 2, 200)

        val iniziali = listOf(
            Uscita(
                id = 1,
                nome = "Gita al Lago",
                gruppoId = 1,
                partecipanti = listOf(amico1, amico2),
                kmTotali = 120,
                guidatori = listOf(amico1)
            ),
            Uscita(
                id = 2,
                nome = "Weekend in Montagna",
                gruppoId = 1,
                partecipanti = listOf(amico1),
                kmTotali = 200,
                guidatori = listOf(amico1)
            )
        )

        _uscite.value = iniziali
    }

    fun getTutteLeUscite(): List<Uscita> = _uscite.value

    fun aggiungiUscita(uscita: Uscita) {
        _uscite.value = _uscite.value + uscita
    }

    fun aggiornaUscita(uscita: Uscita) {
        _uscite.value = _uscite.value.map {
            if (it.id == uscita.id) uscita else it
        }
    }

    fun eliminaUscita(uscitaId: Int) {
        _uscite.value = _uscite.value.filterNot { it.id == uscitaId }
    }

    fun getUscitePerGruppo(gruppoId: Int): List<Uscita> {
        return _uscite.value.filter { it.gruppoId == gruppoId }
    }

    fun getUscitaPerId(id: Int): Uscita? {
        return _uscite.value.find { it.id == id }
    }

    fun eliminaUscitePerGruppo(gruppoId: Int) {
        _uscite.value = _uscite.value.filterNot { it.gruppoId == gruppoId }
    }

    fun generaNuovoId(): Int {
        return (_uscite.value.maxOfOrNull { it.id } ?: 0) + 1
    }
}
