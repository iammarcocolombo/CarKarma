package it.col.mar.android.carkarma.data.database

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import it.col.mar.android.carkarma.data.model.Uscita
import it.col.mar.android.carkarma.domain.repository.UscitaRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class UscitaRepositoryImpl(private val db: FirebaseFirestore) : UscitaRepository {

    override fun getUsciteDelGruppo(gruppoId: String): Flow<List<Uscita>> = callbackFlow {
        val registration = db.collection("gruppi").document(gruppoId).collection("uscite")
            .orderBy("data", Query.Direction.DESCENDING)
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

    override suspend fun getUsciteSnapshot(gruppoId: String): List<Uscita> {
        return try {
            val snapshot = db.collection("gruppi").document(gruppoId)
                .collection("uscite").get().await()
            snapshot.toObjects<Uscita>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun getUscita(gruppoId: String, uscitaId: String): Uscita? {
        return try {
            val snapshot = db.collection("gruppi").document(gruppoId)
                .collection("uscite").document(uscitaId).get().await()
            snapshot.toObject<Uscita>()
        } catch (_: Exception) {
            null
        }
    }

    override fun aggiungiUscita(uscita: Uscita) {
        val idFinale = uscita.id.ifEmpty { UUID.randomUUID().toString() }
        val uscitaDaSalvare = uscita.copy(id = idFinale)

        db.collection("gruppi").document(uscita.gruppoId)
            .collection("uscite").document(idFinale)
            .set(uscitaDaSalvare)
    }

    override fun aggiornaUscita(uscita: Uscita) {
        aggiungiUscita(uscita)
    }

    override fun eliminaUscita(gruppoId: String, uscitaId: String) {
        db.collection("gruppi").document(gruppoId)
            .collection("uscite").document(uscitaId)
            .delete()
    }

    override fun eliminaTutteUsciteDelGruppo(gruppoId: String) {
        val collectionRef = db.collection("gruppi").document(gruppoId).collection("uscite")
        collectionRef.get().addOnSuccessListener { snapshot ->
            for (doc in snapshot.documents) {
                doc.reference.delete()
            }
        }
    }
}