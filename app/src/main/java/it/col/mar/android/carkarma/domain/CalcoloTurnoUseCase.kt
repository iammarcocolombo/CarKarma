package it.col.mar.android.carkarma.domain

import it.col.mar.android.carkarma.data.model.Amico

class CalcoloTurnoUseCase {

    companion object {
        private const val STANDARD_CONSUMO = 6.0 // L/100km
        private const val STANDARD_PREZZO = 1.80 // Euro/L
    }

    /**
     * QUERY: Calcola chi deve guidare basandosi sulla rete dei debiti "Peer-to-Peer"
     */
    fun calcolaChiGuida(
        membriGruppo: List<Amico>,
        presentiIds: Set<String>
    ): List<Pair<Amico, Double>> {
        val presenti = membriGruppo.filter { presentiIds.contains(it.id) }
        val numeroPersone = presenti.size

        if (numeroPersone == 0) return emptyList()
        if (numeroPersone == 1) return listOf(presenti.first() to 0.0)

        val classificaBase = calcolaBilanciPresenti(presenti)
        return selezionaGuidatoriPerCapienza(classificaBase, numeroPersone)
    }

    private fun calcolaBilanciPresenti(presenti: List<Amico>): List<Pair<Amico, Double>> {
        return presenti.map { candidato ->
            var bilancioNettoVersoIPresenti = 0.0

            for (altro in presenti) {
                if (candidato.id != altro.id) {
                    bilancioNettoVersoIPresenti += candidato.bilanci[altro.id] ?: 0.0
                }
            }
            candidato to bilancioNettoVersoIPresenti
        }.sortedBy { it.second }
    }

    private fun selezionaGuidatoriPerCapienza(
        classifica: List<Pair<Amico, Double>>,
        numeroPersone: Int
    ): List<Pair<Amico, Double>> {
        val candidatiUnici = classifica.filter { it.first.postiAuto >= numeroPersone }
        if (candidatiUnici.isNotEmpty()) return candidatiUnici

        val guidatoriScelti = mutableListOf<Pair<Amico, Double>>()
        var postiCoperti = 0

        for (candidato in classifica) {
            guidatoriScelti.add(candidato)
            postiCoperti += candidato.first.postiAuto
            if (postiCoperti >= numeroPersone) break
        }

        return guidatoriScelti
    }

    /**
     * COMMAND: Calcola il costo esatto in EURO per 1 solo Kilometro.
     */
    fun calcolaCostoChilometrico(amico: Amico, prezzi: Map<String, Double>): Double {
        val consumoUtente = if (amico.consumoMedio > 0.0) amico.consumoMedio else STANDARD_CONSUMO
        val prezzoAlLitro = prezzi.entries
            .firstOrNull { it.key.equals(amico.tipoCarburante, ignoreCase = true) }
            ?.value ?: STANDARD_PREZZO

        return (consumoUtente / 100.0) * prezzoAlLitro
    }
}