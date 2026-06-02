package it.col.mar.android.carkarma.domain.repository

/**
 * Interfaccia del Repository per la gestione dei prezzi dei carburanti.
 * Definita nel Domain Layer per disaccoppiare la logica di calcolo dai dettagli tecnologici di recupero dati (API/Firebase).
 */
interface CarburanteRepository {
    /**
     * Recupera la mappa aggiornata dei prezzi dei carburanti (in Euro/litro o Euro/kWh).
     */
    suspend fun getPrezziAggiornati(): Map<String, Double>
}