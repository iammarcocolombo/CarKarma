package it.col.mar.android.carkarma.domain

import it.col.mar.android.carkarma.data.model.Amico

class CalcoloTurnoUseCase {

    /**
     * Calcola dinamicamente quanti guidatori servono.
     *
     * Regole:
     * 1. Se esiste un'auto singola che può portare tutti, sceglie quella (rispettando il Karma).
     * 2. Se non esiste, somma le auto dei debitori (Karma peggiore) finché non copre tutti i posti.
     */
    fun calcolaChiGuida(
        membriGruppo: List<Amico>,
        presentiIds: Set<String>
    ): List<Pair<Amico, Double>> {

        // 1. Identifichiamo chi è presente
        val presenti = membriGruppo.filter { presentiIds.contains(it.id) }
        val numeroPersone = presenti.size

        if (numeroPersone < 2) return emptyList()

        // 2. Statistiche Gruppo per il calcolo del Karma
        val kmTotaliGruppo = membriGruppo.sumOf { it.km }
        val presenzeTotaliGruppo = membriGruppo.sumOf { it.uscite }

        val costoMedioPerPresenza = if (presenzeTotaliGruppo > 0) {
            kmTotaliGruppo.toDouble() / presenzeTotaliGruppo
        } else {
            0.0
        }

        // 3. Classifica base: Tutti ordinati per debito (dal più indebitato al più virtuoso)
        val classificaBase = presenti.map { amico ->
            val kmDovuti = amico.uscite * costoMedioPerPresenza
            val karma = amico.km - kmDovuti
            amico to karma
        }.sortedBy { it.second }

        // --- FASE DECISIONALE DINAMICA ---

        // TENTATIVO A: Basta una macchina sola?
        // Cerchiamo tra i presenti se qualcuno ha un'auto capiente >= numeroPersone
        val candidatiUnici = classificaBase.filter { (amico, _) ->
            amico.postiAuto >= numeroPersone
        }

        if (candidatiUnici.isNotEmpty()) {
            // Sì! Prendiamo il primo (quello con Karma peggiore) che ha la macchina adatta.
            // Esempio: Siamo in 5. Marco (Karma -100, 4 posti) viene saltato. Luca (Karma -90, 5 posti) viene scelto.
            return listOf(candidatiUnici.first())
        }

        // TENTATIVO B: Servono più macchine (es. siamo in 13, o in 5 ma tutti con Smart)
        val guidatoriScelti = mutableListOf<Pair<Amico, Double>>()
        var postiCoperti = 0

        // Scorriamo la classifica dal debitore peggiore e aggiungiamo macchine finché non copriamo tutti
        for (candidato in classificaBase) {
            guidatoriScelti.add(candidato)
            postiCoperti += candidato.first.postiAuto // Aggiungiamo i posti di questa auto

            if (postiCoperti >= numeroPersone) {
                // Abbiamo coperto tutti i passeggeri!
                break
            }
        }

        // Se anche usando TUTTE le auto non copriamo i posti, restituiamo comunque la lista completa
        // (l'utente capirà che mancano posti, o si stringeranno!)
        return guidatoriScelti
    }
}