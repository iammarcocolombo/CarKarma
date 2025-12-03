package it.col.mar.android.carkarma.data.database

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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

    /**
     * Ascolta in tempo reale le uscite di uno specifico gruppo (Sottocollezione).
     * ORDINE: Cronologico inverso (dalla più recente alla più vecchia).
     */
    fun getUsciteDelGruppo(gruppoId: String): Flow<List<Uscita>> = callbackFlow {
        val registration = db.collection("gruppi").document(gruppoId).collection("uscite")
            .orderBy("data", Query.Direction.DESCENDING) // <--- ORDINAMENTO CRONOLOGICO
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
     * Recupera una singola uscita specifica (serve sapere il gruppoId per trovarla).
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
        // Se è una nuova uscita, il model ha già il timestamp 'data' impostato a now().
        // Se è una modifica, manteniamo la data originale dell'oggetto.
        val uscitaDaSalvare = uscita.copy(id = idFinale)

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

    fun eliminaTutteUsciteDelGruppo(gruppoId: String) {
        val collectionRef = db.collection("gruppi").document(gruppoId).collection("uscite")
        collectionRef.get().addOnSuccessListener { snapshot ->
            for (doc in snapshot.documents) {
                doc.reference.delete()
            }
        }
    }
}