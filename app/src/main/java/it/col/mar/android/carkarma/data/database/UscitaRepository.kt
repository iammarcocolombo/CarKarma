package it.col.mar.android.carkarma.data.database

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import it.col.mar.android.carkarma.data.model.Uscita
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class UscitaRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    // Flow che contiene la lista aggiornata in tempo reale delle uscite
    private val _uscite = MutableStateFlow<List<Uscita>>(emptyList())
    val uscite: StateFlow<List<Uscita>> = _uscite.asStateFlow()

    init {
        // ASCOLTO TEMPO REALE
        // Collega l'app alla collezione "uscite" su Firebase.
        // Ogni volta che qualcuno aggiunge un'uscita, questa lista si aggiorna automaticamente.
        db.collection("uscite")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    // Gestione errore silenziosa per ora
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    // Converte i documenti JSON di Firebase in oggetti Uscita Kotlin
                    _uscite.value = snapshot.toObjects<Uscita>()
                }
            }
    }

    /**
     * Restituisce tutte le uscite scaricate.
     */
    fun getTutteLeUscite(): List<Uscita> = _uscite.value

    /**
     * Filtra le uscite locali per trovare quelle di uno specifico gruppo.
     */
    fun getUscitePerGruppo(gruppoId: String): List<Uscita> {
        return _uscite.value.filter { it.gruppoId == gruppoId }
    }

    /**
     * Cerca un'uscita specifica per ID.
     */
    fun getUscitaPerId(id: String): Uscita? {
        return _uscite.value.find { it.id == id }
    }

    /**
     * Salva una nuova uscita o ne aggiorna una esistente su Firebase.
     */
    fun aggiungiUscita(uscita: Uscita) {
        // Se l'ID è vuoto, è una nuova uscita -> generiamo un UUID
        val idFinale = if (uscita.id.isEmpty()) UUID.randomUUID().toString() else uscita.id

        // Assicuriamoci che l'oggetto da salvare abbia l'ID corretto
        val uscitaDaSalvare = uscita.copy(id = idFinale)

        // Scrittura su Firestore (path: uscite/{idFinale})
        db.collection("uscite").document(idFinale).set(uscitaDaSalvare)
    }

    /**
     * Aggiorna un'uscita (in Firestore è uguale ad aggiungi/sovrascrivi).
     */
    fun aggiornaUscita(uscita: Uscita) {
        aggiungiUscita(uscita)
    }

    /**
     * Elimina un'uscita dal database.
     */
    fun eliminaUscita(uscitaId: String) {
        db.collection("uscite").document(uscitaId).delete()
    }

    /**
     * Elimina tutte le uscite associate a un gruppo.
     * Nota: Firestore non supporta l'eliminazione "per query" direttamente dal client in modo semplice.
     * Iteriamo sui documenti locali e li cancelliamo uno per uno.
     */
    fun eliminaUscitePerGruppo(gruppoId: String) {
        val usciteDaEliminare = _uscite.value.filter { it.gruppoId == gruppoId }
        for (uscita in usciteDaEliminare) {
            eliminaUscita(uscita.id)
        }
    }

    // Helper per compatibilità, anche se ora generiamo l'ID in aggiungiUscita
    fun generaNuovoId(): String {
        return UUID.randomUUID().toString()
    }
}