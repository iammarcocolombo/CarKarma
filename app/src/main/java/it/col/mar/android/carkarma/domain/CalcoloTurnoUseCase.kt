package it.col.mar.android.carkarma.domain

import it.col.mar.android.carkarma.data.model.Amico
import kotlin.math.max

class CalcoloTurnoUseCase {

    // Definiamo un consumo standard di riferimento (es. 14 km/litro per una media benzina)
    // Questo serve per normalizzare i punteggi.
    private val STANDARD_KM_LITRO = 14.0

    /**
     * Calcola dinamicamente quanti guidatori servono basandosi sul "Karma Pesato".
     * Il Karma ora tiene conto anche dell'efficienza dell'auto (consumi).
     */
    fun calcolaChiGuida(
        membriGruppo: List<Amico>,
        presentiIds: Set<String>
    ): List<Pair<Amico, Double>> {

        // 1. Identifichiamo chi è presente
        val presenti = membriGruppo.filter { presentiIds.contains(it.id) }
        val numeroPersone = presenti.size

        if (numeroPersone < 2) return emptyList()

        // 2. Calcolo dei "Km Equivalenti" (Peso economico) per ogni membro
        // Se hai guidato 100km con una Ferrari (che consuma tanto), valgono come 200km di una Panda.
        val mappaKmEquivalenti = membriGruppo.associate { amico ->
            amico.id to calcolaKmEquivalenti(amico)
        }

        // 3. Statistiche Gruppo per il calcolo della media
        val totaleKmEquivalentiGruppo = mappaKmEquivalenti.values.sum()
        val presenzeTotaliGruppo = membriGruppo.sumOf { it.uscite }

        // Il "Costo medio" per ogni uscita fatta.
        // Chi è uscito 10 volte "deve" al gruppo 10 * costoMedio.
        val debitoPerPresenza = if (presenzeTotaliGruppo > 0) {
            totaleKmEquivalentiGruppo / presenzeTotaliGruppo
        } else {
            0.0
        }

        // 4. Classifica Karma Pesato
        // Karma = (Quanto ho dato * FattoreAuto) - (Quanto ho ricevuto * MediaGruppo)
        val classificaBase = presenti.map { amico ->
            val kmFattiPesati = mappaKmEquivalenti[amico.id] ?: 0.0
            val debitoAccumulato = amico.uscite * debitoPerPresenza

            // KARMA: Più è basso, più sei in debito (e devi guidare)
            val karma = kmFattiPesati - debitoAccumulato

            amico to karma
        }.sortedBy { it.second } // Ordinati dal Karma peggiore (più basso) a salire

        // --- FASE DECISIONALE DINAMICA (LOGICA AUTO) ---

        // TENTATIVO A: Basta una macchina sola?
        val candidatiUnici = classificaBase.filter { (amico, _) ->
            amico.postiAuto >= numeroPersone
        }

        if (candidatiUnici.isNotEmpty()) {
            // Prendiamo il primo della lista (quello con Karma più basso/peggiore)
            return listOf(candidatiUnici.first())
        }

        // TENTATIVO B: Servono più macchine
        val guidatoriScelti = mutableListOf<Pair<Amico, Double>>()
        var postiCoperti = 0

        for (candidato in classificaBase) {
            guidatoriScelti.add(candidato)
            postiCoperti += candidato.first.postiAuto

            if (postiCoperti >= numeroPersone) {
                break
            }
        }

        return guidatoriScelti
    }

    /**
     * Calcola i km "pesati" in base al consumo dell'auto.
     * Logica:
     * - Consumo Standard (14 km/l) -> Moltiplicatore 1.0
     * - Consumo Alto (es. 7 km/l) -> Moltiplicatore 2.0 (I tuoi km valgono doppio perché spendi il doppio)
     * - Consumo Basso (es. 28 km/l) -> Moltiplicatore 0.5
     */
    private fun calcolaKmEquivalenti(amico: Amico): Double {
        val kmReali = amico.km.toDouble()

        // Se l'utente non ha impostato il consumo, usiamo lo standard (fattore 1.0)
        if (amico.consumoMedio <= 0.0) return kmReali

        // Normalizzazione per auto Elettriche (opzionale, per ora le trattiamo in base al numero inserito)
        // Se volessimo essere precisi sui costi, qui dovremmo convertire km/kWh in km/L equivalenti.
        // Per ora usiamo la logica pura dell'efficienza inserita dall'utente.

        val efficienzaUtente = amico.consumoMedio // Km al Litro (o Km al kWh)

        // Formula inversa: Meno km fai con un litro, più alto è il peso del tuo sacrificio
        val fattoreAuto = STANDARD_KM_LITRO / efficienzaUtente

        return kmReali * fattoreAuto
    }
}