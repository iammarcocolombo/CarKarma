package it.col.mar.android.carkarma.data.database

import it.col.mar.android.carkarma.data.model.Gruppo
import it.col.mar.android.carkarma.data.model.Amico

class GruppoRepository {

    private val gruppi = mutableListOf<Gruppo>()

    init {
        // Gruppi di esempio all'avvio
        val amico1 = Amico(1, "Marco", 2, 5, 3, 300)
        val amico2 = Amico(2, "Luca", 1, 3, 2, 200)
        val gruppo = Gruppo(1, "Amici", listOf(amico1, amico2))

        gruppi.add(gruppo)
    }

    fun getTuttiIGruppi(): List<Gruppo> = gruppi

    fun aggiungiGruppo(gruppo: Gruppo) {
        gruppi.add(gruppo)
    }

    fun rimuoviGruppo(gruppo: Gruppo) {
        gruppi.remove(gruppo)
    }

    fun getGruppoPerId(id: Int): Gruppo? {
        return gruppi.find { it.id == id }
    }
}
