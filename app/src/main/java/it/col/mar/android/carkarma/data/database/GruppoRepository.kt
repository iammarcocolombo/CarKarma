package it.col.mar.android.carkarma.data.database

import it.col.mar.android.carkarma.data.model.Gruppo
import it.col.mar.android.carkarma.data.model.Amico

class GruppoRepository(private val amicoRepository: AmicoRepository) {

    private val gruppi = mutableListOf<Gruppo>()

    init {
        val amici = amicoRepository.getTuttiGliAmici()

        val gruppo1 = Gruppo(1, "Gruppo Completo", amici) // tutti e 3
        val gruppo2 = Gruppo(2, "Gruppo Marco e Luca", amici.filter { it.id == 1 || it.id == 2 })
        val gruppo3 = Gruppo(3, "Gruppo Luca e Giulia", amici.filter { it.id == 2 || it.id == 3 })

        gruppi.addAll(listOf(gruppo1, gruppo2, gruppo3))
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
