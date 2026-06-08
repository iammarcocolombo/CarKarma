package it.col.mar.android.carkarma.domain.repository

/**
 * Interfaccia del Repository per la gestione dei prezzi dei carburanti.
 * Definita nel Domain Layer per disaccoppiare la logica di calcolo dai dettagli tecnologici di recupero dati (API/Firebase).
 */
interface CarburanteRepository {
    suspend fun getPrezziAggiornati(): Map<String, Double>
}