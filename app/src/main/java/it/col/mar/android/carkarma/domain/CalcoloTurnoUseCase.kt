package it.col.mar.android.carkarma.domain

import it.col.mar.android.carkarma.data.model.Amico

class CalcoloTurnoUseCase {

    companion object {
        private const val STANDARD_CONSUMO = 6.0 // L/100km
        private const val STANDARD_PREZZO = 1.80 // Euro/L
    }

    /**
     * QUERY: Calcola chi deve guidare basandosi sulla rete dei debiti "Peer-to-Peer"
     * e tenendo conto sia dei membri del gruppo che dei loro ospiti.
     */
    fun calcolaChiGuida(
        membriGruppo: List<Amico>,
        presentiIds: Set<String>,
        ospiti: Map<String, Int> = emptyMap()
    ): List<Pair<Amico, Double>> {
        val presenti = membriGruppo.filter { presentiIds.contains(it.id) }
        val numeroMembri = presenti.size

        if (numeroMembri == 0) return emptyList()

        // Somma delle persone totali = membri presenti + i loro ospiti
        val numeroOspitiTotali = ospiti.filterKeys { presentiIds.contains(it) }.values.sum()
        val numeroPersoneTotali = numeroMembri + numeroOspitiTotali

        if (numeroPersoneTotali == 1) return listOf(presenti.first() to 0.0)

        val classificaBase = calcolaBilanciPresenti(presenti)
        return selezionaGuidatoriPerCapienza(classificaBase, numeroPersoneTotali)
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
        }.sortedBy { it.second } // Ordina dal bilancio più in debito
    }

    private fun selezionaGuidatoriPerCapienza(
        classifica: List<Pair<Amico, Double>>,
        numeroPersoneTotali: Int
    ): List<Pair<Amico, Double>> {
        // Se un'unica auto può contenere TUTTE le persone (membri + ospiti), basta il primo in classifica
        val candidatiUnici = classifica.filter { it.first.postiAuto >= numeroPersoneTotali }
        if (candidatiUnici.isNotEmpty()) {
            return listOf(candidatiUnici.first())
        }

        // Altrimenti selezioniamo più auto finché i posti coperti coprono tutte le persone
        val guidatoriScelti = mutableListOf<Pair<Amico, Double>>()
        var postiCoperti = 0

        for (candidato in classifica) {
            guidatoriScelti.add(candidato)
            postiCoperti += candidato.first.postiAuto
            if (postiCoperti >= numeroPersoneTotali) break
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