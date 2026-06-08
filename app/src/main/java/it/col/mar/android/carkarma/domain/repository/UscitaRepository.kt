package it.col.mar.android.carkarma.domain.repository

import it.col.mar.android.carkarma.data.model.Uscita
import kotlinx.coroutines.flow.Flow

interface UscitaRepository {
    fun getUsciteDelGruppo(gruppoId: String): Flow<List<Uscita>>
    suspend fun getUsciteSnapshot(gruppoId: String): List<Uscita>
    suspend fun getUscita(gruppoId: String, uscitaId: String): Uscita?
    fun aggiungiUscita(uscita: Uscita)
    fun aggiornaUscita(uscita: Uscita)
    fun eliminaUscita(gruppoId: String, uscitaId: String)
    fun eliminaTutteUsciteDelGruppo(gruppoId: String)

}