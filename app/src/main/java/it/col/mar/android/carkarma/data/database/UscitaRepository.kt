package it.col.mar.android.carkarma.data.database

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import it.col.mar.android.carkarma.data.model.Uscita
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class UscitaRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    // NON carichiamo più tutte le uscite del mondo in una lista globale.
    // Usiamo i Flow per ascoltare solo quello che ci serve.

    /**
     * Ascolta in tempo reale le uscite di uno specifico gruppo (Sottocollezione).
     */
    fun getUsciteDelGruppo(gruppoId: String): Flow<List<Uscita>> = callbackFlow {
        val registration = db.collection("gruppi").document(gruppoId).collection("uscite")
            // Ordiniamo per data (se hai un campo data, altrimenti per nome o id)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val uscite = snapshot.toObjects<Uscita>()
                    trySend(uscite)
                }
            }
        awaitClose { registration.remove() }
    }

    /**
     * Recupera una singola uscita (serve sapere il gruppoId per trovarla).
     */
    suspend fun getUscita(gruppoId: String, uscitaId: String): Uscita? {
        return try {
            val snapshot = db.collection("gruppi").document(gruppoId)
                .collection("uscite").document(uscitaId).get().await()
            snapshot.toObject<Uscita>()
        } catch (e: Exception) {
            null
        }
    }

    fun aggiungiUscita(uscita: Uscita) {
        val idFinale = if (uscita.id.isEmpty()) UUID.randomUUID().toString() else uscita.id
        val uscitaDaSalvare = uscita.copy(id = idFinale)

        // Salviamo nella SOTTOCOLLEZIONE: gruppi/{id}/uscite/{id}
        db.collection("gruppi").document(uscita.gruppoId)
            .collection("uscite").document(idFinale)
            .set(uscitaDaSalvare)
    }

    fun aggiornaUscita(uscita: Uscita) {
        aggiungiUscita(uscita)
    }

    fun eliminaUscita(gruppoId: String, uscitaId: String) {
        db.collection("gruppi").document(gruppoId)
            .collection("uscite").document(uscitaId)
            .delete()
    }

    // Questa funzione viene chiamata da GruppoRepository quando elimini un gruppo
    fun eliminaTutteUsciteDelGruppo(gruppoId: String) {
        val collectionRef = db.collection("gruppi").document(gruppoId).collection("uscite")

        // Firestore richiede di scaricare i documenti per cancellarli uno a uno
        collectionRef.get().addOnSuccessListener { snapshot ->
            for (doc in snapshot.documents) {
                doc.reference.delete()
            }
        }
    }
}