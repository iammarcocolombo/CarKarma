package it.col.mar.android.carkarma.domain

import it.col.mar.android.carkarma.data.model.Amico

class CalcoloTurnoUseCase {

    companion object {
        private const val STANDARD_CONSUMO = 6.0 // L/100km
        private const val STANDARD_PREZZO = 1.80 // Euro/L
    }

    /**
     * QUERY: Calcola chi deve guidare basandosi sulla rete dei debiti "Peer-to-Peer" (stile Splitwise).
     * Calcola il bilancio netto di ogni candidato SOLO verso le altre persone fisicamente presenti stasera.
     */
    fun calcolaChiGuida(
        membriGruppo: List<Amico>,
        presentiIds: Set<String>
    ): List<Pair<Amico, Double>> {

        // 1. Isoliamo solo chi è fisicamente presente all'uscita di stasera
        val presenti = membriGruppo.filter { presentiIds.contains(it.id) }
        val numeroPersone = presenti.size

        if (numeroPersone == 0) return emptyList()
        if (numeroPersone == 1) return listOf(presenti.first() to 0.0)

        // 2. Calcolo del Bilancio di Rete (Peer-to-Peer)
        // Per ogni persona presente, calcoliamo il suo stato patrimoniale SOLO verso gli altri presenti.
        val classificaBase = presenti.map { candidato ->
            var bilancioNettoVersoIPresenti = 0.0

            // Esaminiamo i debiti/crediti che questo candidato ha con gli altri presenti
            for (altro in presenti) {
                if (candidato.id != altro.id) {
                    // Preleviamo il debito specifico verso l'altra persona.
                    // Se positivo = l'altro mi deve soldi (credito). Se negativo = io devo soldi all'altro (debito).
                    // Nota: 'bilanci' è la nuova mappa che aggiungeremo nel modello Amico.
                    val saldoSpecifico = candidato.bilanci[altro.id] ?: 0.0
                    bilancioNettoVersoIPresenti += saldoSpecifico
                }
            }

            candidato to bilancioNettoVersoIPresenti
        }.sortedBy { it.second } // Ordiniamo dal più in debito (valore negativo) al più in credito (valore positivo)

        // --- FASE DECISIONALE DINAMICA (Capacità veicolo) ---
        // Cerchiamo chi ha l'auto abbastanza grande partendo da chi è più in debito
        val candidatiUnici = classificaBase.filter { (amico, _) ->
            amico.postiAuto >= numeroPersone
        }

        if (candidatiUnici.isNotEmpty()) {
            return candidatiUnici
        }

        // Se nessuna auto singola basta, accumuliamo i guidatori necessari
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
     * COMMAND: Calcola il costo esatto in EURO per 1 solo Chilometro.
     */
    fun calcolaCostoChilometrico(amico: Amico, prezzi: Map<String, Double>): Double {
        val consumoUtente = if (amico.consumoMedio > 0.0) amico.consumoMedio else STANDARD_CONSUMO
        val prezzoAlLitro = prezzi.entries
            .firstOrNull { it.key.equals(amico.tipoCarburante, ignoreCase = true) }
            ?.value ?: STANDARD_PREZZO

        return (consumoUtente / 100.0) * prezzoAlLitro
    }
}