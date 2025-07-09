package it.col.mar.android.carkarma.data.database

import it.col.mar.android.carkarma.data.model.Amico
import it.col.mar.android.carkarma.data.model.Uscita

class UscitaRepository {

    private val uscite = mutableListOf<Uscita>()

    init {
        val amico1 = Amico(1, "Marco", 2, 5, 3, 300)
        val amico2 = Amico(2, "Luca", 1, 3, 2, 200)

        uscite.add(
            Uscita(
                id = 1,
                nome = "Gita al Lago",
                gruppoId = 1,
                partecipanti = listOf(amico1, amico2),
                kmTotali = 120,
                guidatori = listOf(amico1)
            )
        )
        uscite.add(
            Uscita(
                id = 2,
                nome = "Weekend in Montagna",
                gruppoId = 1,
                partecipanti = listOf(amico1),
                kmTotali = 200,
                guidatori = listOf(amico1)
            )
        )
    }

    fun getTutteLeUscite(): List<Uscita> {
        return uscite
    }

    fun aggiungiUscita(uscita: Uscita) {
        uscite.add(uscita)
    }

    fun rimuoviUscita(uscita: Uscita) {
        uscite.remove(uscita)
    }

    fun getUscitePerGruppo(gruppoId: Int): List<Uscita> {
        return uscite.filter { it.gruppoId == gruppoId }
    }

    fun getUscitaPerId(id: Int): Uscita? {
        return uscite.find { it.id == id }
    }
}
