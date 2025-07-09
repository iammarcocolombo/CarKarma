package it.col.mar.android.carkarma.data.database

import it.col.mar.android.carkarma.data.model.Amico

class AmicoRepository {

    private val amici = mutableListOf<Amico>()

    init {
        amici.add(Amico(1, "Marco", 2, 5, 3, 300))
        amici.add(Amico(2, "Luca", 1, 3, 2, 200))
        amici.add(Amico(3, "Giulia", 2, 4, 1, 150))
    }

    fun getTuttiGliAmici(): List<Amico> {
        return amici
    }

    fun aggiungiAmico(amico: Amico) {
        amici.add(amico)
    }

    fun rimuoviAmico(amico: Amico) {
        amici.remove(amico)
    }

    fun getAmicoPerId(id: Int): Amico? {
        return amici.find { it.id == id }
    }
}
