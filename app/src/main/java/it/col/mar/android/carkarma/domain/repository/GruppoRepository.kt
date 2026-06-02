package it.col.mar.android.carkarma.domain.repository

import android.net.Uri
import it.col.mar.android.carkarma.data.model.Amico
import it.col.mar.android.carkarma.data.model.Gruppo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interfaccia del Repository per la gestione dei Gruppi e dei relativi Membri.
 * Definita nel Domain Layer per rimuovere la dipendenza tecnologica dai ViewModels.
 */
interface GruppoRepository {
    val gruppi: StateFlow<List<Gruppo>>
    val isDataLoaded: StateFlow<Boolean>

    suspend fun sincronizzaMembriInRubrica(listaGruppi: List<Gruppo>)
    suspend fun uploadImmagineGruppo(gruppoId: String, uriImmagine: Uri): String?
    fun getMembriDelGruppo(gruppoId: String): Flow<List<Amico>>
    suspend fun getMembriSnapshot(gruppoId: String): List<Amico>
    suspend fun getMembro(gruppoId: String, amicoId: String): Amico?
    fun aggiungiMembroAlGruppo(gruppoId: String, amicoTemplate: Amico)
    fun rimuoviMembroDalGruppo(gruppoId: String, amicoId: String)
    fun aggiornaStatisticheMembro(
        gruppoId: String,
        amicoId: String,
        deltaUscite: Int,
        deltaGuide: Int,
        deltaKm: Int,
        deltaKarma: Double
    )
    fun aggiornaAnagraficaMembro(gruppoId: String, amico: Amico)
    fun getTuttiIGruppi(): List<Gruppo>
    fun getGruppoPerId(id: String): Gruppo?
    fun aggiungiGruppo(gruppo: Gruppo)
    fun aggiornaGruppo(gruppo: Gruppo)
    fun eliminaGruppo(gruppoId: String)
    fun lasciaGruppo(gruppoId: String, onResult: (Boolean) -> Unit)
    fun generaNuovoId(): String
    fun uniscitiAlGruppo(gruppoId: String, onResult: (Boolean) -> Unit)
    fun rendiUtenteCercabile(uid: String, email: String?, nome: String?)
    suspend fun eliminaDatiUtentePubblico()
    suspend fun rimuoviUtenteDaTuttiIGruppi()
}