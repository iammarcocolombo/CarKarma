package it.col.mar.android.carkarma.domain

import it.col.mar.android.carkarma.data.model.Amico
import kotlin.math.max

class CalcoloTurnoUseCase {

    // CONSUMO STANDARD DI RIFERIMENTO (Litri per 100km)
    // 7.0 L/100km corrisponde circa ai vecchi 14 km/l.
    private val STANDARD_CONSUMO = 7.0

    // Prezzo standard se l'API fallisce
    private val STANDARD_PREZZO = 1.80

    /**
     * Calcola chi deve guidare basandosi sul Karma ACCUMULATO nel tempo.
     *
     * @param prezziCarburante Mappa con i prezzi (es. "Benzina" -> 1.859).
     * È questo il parametro che mancava e causava l'errore!
     */
    fun calcolaChiGuida(
        membriGruppo: List<Amico>,
        presentiIds: Set<String>,
        prezziCarburante: Map<String, Double> = emptyMap()
    ): List<Pair<Amico, Double>> {

        val presenti = membriGruppo.filter { presentiIds.contains(it.id) }
        val numeroPersone = presenti.size

        if (numeroPersone < 2) return emptyList()

        // 1. Recuperiamo il Karma Storico
        val mappaKarma = presenti.associate { amico ->
            val valoreKarmaReale = if (amico.karma != 0.0) amico.karma else amico.km.toDouble()
            amico.id to valoreKarmaReale
        }

        // 2. Calcolo Media del Gruppo
        val totaleKarmaGruppo = mappaKarma.values.sum()
        val presenzeTotaliGruppo = membriGruppo.sumOf { it.uscite }

        val debitoPerPresenza = if (presenzeTotaliGruppo > 0) {
            totaleKarmaGruppo / presenzeTotaliGruppo
        } else {
            0.0
        }

        // 3. Classifica Bilancio
        val classificaBase = presenti.map { amico ->
            val mioKarma = mappaKarma[amico.id] ?: 0.0
            val mioDebito = amico.uscite * debitoPerPresenza
            val bilancio = mioKarma - mioDebito
            amico to bilancio
        }.sortedBy { it.second }

        // 4. Selezione Guidatori
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
     * Calcola il "Moltiplicatore" per il viaggio di OGGI.
     * Serve al ViewModel per sapere quanti punti aggiungere.
     */
    fun calcolaFattoreAttuale(amico: Amico, prezzi: Map<String, Double>): Double {
        if (amico.consumoMedio <= 0.0) return 1.0

        val consumoUtente = amico.consumoMedio // Litri per 100km

        // Fattore Consumo: Più alto è (più beve l'auto), più punti meriti
        val fattoreConsumo = consumoUtente / STANDARD_CONSUMO

        // Fattore Prezzo: Più costa al litro, più punti meriti
        val prezzoAlLitro = prezzi[amico.tipoCarburante] ?: STANDARD_PREZZO
        val fattorePrezzo = prezzoAlLitro / STANDARD_PREZZO

        return fattoreConsumo * fattorePrezzo
    }
}