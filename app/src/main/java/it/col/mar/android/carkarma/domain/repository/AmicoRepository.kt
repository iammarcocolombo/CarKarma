package it.col.mar.android.carkarma.domain.repository

import it.col.mar.android.carkarma.data.model.Amico
import kotlinx.coroutines.flow.StateFlow

/**
 * Interfaccia di Dominio per la gestione della rubrica amici.
 */
interface AmicoRepository {
    val amici: StateFlow<List<Amico>>

    fun getTuttiGliAmici(): List<Amico>

    suspend fun getAmicoPerId(id: String): Amico?

    suspend fun aggiungiAmico(amico: Amico)
    fun importaAmici(listaAmici: List<Amico>)
    suspend fun rimuoviAmico(amicoId: String)
    suspend fun aggiornaStatisticheAmico(amicoId: String, kmAggiunti: Int, haGuidato: Boolean)
}