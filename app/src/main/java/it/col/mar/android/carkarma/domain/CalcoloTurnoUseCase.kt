package it.col.mar.android.carkarma.domain

import it.col.mar.android.carkarma.data.model.Amico

class CalcoloTurnoUseCase {

    /**
     * @param membriGruppo: Tutti i membri del gruppo
     * @param presentiIds: Gli ID degli amici che sono presenti OGGI
     * @return Lista di coppie (Amico, Punteggio Karma), ordinata da chi deve guidare
     */
    fun calcolaChiGuida(
        membriGruppo: List<Amico>,
        presentiIds: Set<String>
    ): List<Pair<Amico, Double>> {

        // 1. Consideriamo solo chi è presente stasera
        val presenti = membriGruppo.filter { presentiIds.contains(it.id) }

        if (presenti.isEmpty()) return emptyList()

        // 2. Filtro Posti Auto (Opzionale, per ora semplice)
        // Se siamo in 5 e uno ha la Smart, andrebbe escluso. Per ora lo lasciamo semplice.

        // 3. Calcoliamo i totali del GRUPPO INTERO (non solo dei presenti)
        // Questo serve per capire il "costo medio" della compagnia
        val kmTotaliGruppo = membriGruppo.sumOf { it.km }
        val presenzeTotaliGruppo = membriGruppo.sumOf { it.uscite }

        // Evitiamo divisione per zero
        val costoMedioPerPresenza = if (presenzeTotaliGruppo > 0) {
            kmTotaliGruppo.toDouble() / presenzeTotaliGruppo
        } else {
            0.0
        }

        // 4. Calcoliamo il Karma per i PRESENTI
        val classifica = presenti.map { amico ->
            // Quanto avrebbe dovuto guidare questo amico in un mondo "perfetto"?
            val kmDovuti = amico.uscite * costoMedioPerPresenza

            // Karma = Km Reali - Km Dovuti
            // Esempio: Ha guidato 0km, doveva guidarne 100. Karma = -100 (Deve guidare!)
            // Esempio: Ha guidato 500km, doveva guidarne 100. Karma = +400 (Sta tranquillo)
            val karma = amico.km - kmDovuti

            amico to karma
        }

        // 5. Ordina: Chi ha il karma più basso (più negativo) guida per primo
        return classifica.sortedBy { it.second }
    }
}