package it.col.mar.android.carkarma.domain

import it.col.mar.android.carkarma.data.model.Amico
import kotlin.math.max

class CalcoloTurnoUseCase {

    companion object {
        // Standard abbassato a 6.0 L/100km (più realistico per le auto moderne)
        private const val STANDARD_CONSUMO = 6.0
        private const val STANDARD_PREZZO = 1.80 // Euro
    }

    /**
     * QUERY: Calcola chi deve guidare basandosi sul Bilancio Economico (Euro).
     * OPZIONE A: I guidatori non pagano il debito per quel viaggio, solo i passeggeri.
     */
    fun calcolaChiGuida(
        membriGruppo: List<Amico>,
        presentiIds: Set<String>
    ): List<Pair<Amico, Double>> {

        val presenti = membriGruppo.filter { presentiIds.contains(it.id) }
        val numeroPersone = presenti.size

        if (numeroPersone == 0) return emptyList()
        if (numeroPersone == 1) return listOf(presenti.first() to 0.0)

        // 1. Recuperiamo il Karma (che d'ora in poi rappresenta gli EURO spesi)
        val mappaKarmaTotale = membriGruppo.associate { amico ->
            val valoreKarmaReale = if (amico.karma != 0.0) amico.karma else amico.km.toDouble()
            amico.id to valoreKarmaReale
        }

        val totaleKarmaGruppo = mappaKarmaTotale.values.sum()

        // 2. Calcolo dei Passeggeri Totali (Opzione A)
        // Invece di dividere il costo per tutte le presenze, lo dividiamo solo per le volte
        // in cui qualcuno si è seduto sul sedile del passeggero.
        val presenzeTotaliGruppo = membriGruppo.sumOf { it.uscite }
        val guideTotaliGruppo = membriGruppo.sumOf { it.guide }
        val passeggeriTotali = presenzeTotaliGruppo - guideTotaliGruppo

        // Quanto costa mediamente occupare un sedile da passeggero
        val costoPerPasseggero = if (passeggeriTotali > 0) {
            totaleKarmaGruppo / passeggeriTotali
        } else {
            0.0
        }

        // 3. Classifica Bilancio
        // Bilancio = (Miei Euro Spesi per il gruppo) - (Mio debito accumulato scroccando passaggi)
        val classificaBase = presenti.map { amico ->
            val mioKarma = mappaKarmaTotale[amico.id] ?: 0.0

            // Calcolo quante volte ha fatto il passeggero
            val voltePasseggero = max(0, amico.uscite - amico.guide)
            val mioDebito = voltePasseggero * costoPerPasseggero

            val bilancio = mioKarma - mioDebito

            amico to bilancio
        }.sortedBy { it.second } // Ordinati dal più in debito (deve guidare) al più in credito

        // --- FASE DECISIONALE DINAMICA ---
        val candidatiUnici = classificaBase.filter { (amico, _) ->
            amico.postiAuto >= numeroPersone
        }

        if (candidatiUnici.isNotEmpty()) {
            return listOf(candidatiUnici.first())
        }

        val guidatoriScelti = mutableListOf<Pair<Amico, Double>>()
        var postiCoperti = 0

        for (candidato in classificaBase) {
            guidatoriScelti.add(candidato)
            postiCoperti += candidato.first.postiAuto
            if (postiCoperti >= numeroPersone) break
        }

        return guidatoriScelti
    }

    /**
     * COMMAND: Calcola il costo esatto in EURO per 1 solo Kilometro.
     * Formula: (Consumo / 100) * Prezzo
     */
    fun calcolaCostoChilometrico(amico: Amico, prezzi: Map<String, Double>): Double {
        val consumoUtente = if (amico.consumoMedio > 0.0) amico.consumoMedio else STANDARD_CONSUMO

        val prezzoAlLitro = prezzi.entries
            .firstOrNull { it.key.equals(amico.tipoCarburante, ignoreCase = true) }
            ?.value ?: STANDARD_PREZZO

        // Esempio: 6.0 L/100km -> 0.06 Litri al km.
        // 0.06 * 1.80€ = 0.108 Euro al km.
        return (consumoUtente / 100.0) * prezzoAlLitro
    }
}