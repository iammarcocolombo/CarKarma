package it.col.mar.android.carkarma.domain.repository

import it.col.mar.android.carkarma.data.model.Amico
import kotlinx.coroutines.flow.StateFlow

interface AmicoRepository {
    // Espone il flusso di dati in tempo reale
    val amici: StateFlow<List<Amico>>

    // Recupero dati sincrono dallo stato attuale
    fun getTuttiGliAmici(): List<Amico>
    fun getAmicoPerId(id: String): Amico?

    // Operazioni di scrittura
    suspend fun aggiungiAmico(amico: Amico)
    fun importaAmici(listaAmici: List<Amico>)
    suspend fun rimuoviAmico(amicoId: String)
}