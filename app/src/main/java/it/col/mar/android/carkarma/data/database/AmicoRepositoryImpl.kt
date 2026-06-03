package it.col.mar.android.carkarma.data.database

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import it.col.mar.android.carkarma.data.model.Amico
import it.col.mar.android.carkarma.domain.repository.AmicoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Implementazione reale di AmicoRepository con persistenza Cloud Firestore.
 * Aggiornato per riflettere le corrette firme suspend del Domain Layer.
 */
class AmicoRepositoryImpl(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : AmicoRepository {
    private val _amici = MutableStateFlow<List<Amico>>(emptyList())
    override val amici: StateFlow<List<Amico>> = _amici.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val userId = firebaseAuth.currentUser?.uid
            if (userId != null) {
                db.collection("users").document(userId).collection("amici")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) return@addSnapshotListener
                        if (snapshot != null) {
                            _amici.value = snapshot.toObjects<Amico>()
                        }
                    }
            } else {
                _amici.value = emptyList()
            }
        }
    }

    override fun getTuttiGliAmici(): List<Amico> = _amici.value

    override suspend fun getAmicoPerId(id: String): Amico? {
        val local = _amici.value.find { it.id == id }
        if (local != null) return local

        val userId = auth.currentUser?.uid ?: return null
        return try {
            db.collection("users").document(userId).collection("amici")
                .document(id)
                .get()
                .await()
                .toObject(Amico::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun aggiungiAmico(amico: Amico) {
        val userId = auth.currentUser?.uid ?: return

        val idFinale = if (amico.id.isEmpty()) UUID.randomUUID().toString() else amico.id
        val amicoDaSalvare = amico.copy(id = idFinale)

        try {
            db.collection("users").document(userId).collection("amici")
                .document(idFinale)
                .set(amicoDaSalvare)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun importaAmici(listaAmici: List<Amico>) {
        val userId = auth.currentUser?.uid ?: return
        val collectionRef = db.collection("users").document(userId).collection("amici")
        val batch = db.batch()

        listaAmici.forEach { amico ->
            val template = amico.copy(uscite = 0, guide = 0, km = 0)
            val docRef = collectionRef.document(template.id)
            batch.set(docRef, template)
        }
        batch.commit()
    }

    override suspend fun rimuoviAmico(amicoId: String) {
        val userId = auth.currentUser?.uid ?: return
        try {
            db.collection("users").document(userId).collection("amici")
                .document(amicoId)
                .delete()
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // CORREZIONE CHIAVE: Trasformata in una funzione suspend robusta ed esente da leak di memoria
    override suspend fun aggiornaStatisticheAmico(amicoId: String, kmAggiunti: Int, haGuidato: Boolean) {
        try {
            val amico = getAmicoPerId(amicoId) ?: return
            val amicoAggiornato = amico.copy(
                uscite = amico.uscite + 1,
                guide = if (haGuidato) amico.guide + 1 else amico.guide,
                km = if (haGuidato) amico.km + kmAggiunti else amico.km
            )
            aggiungiAmico(amicoAggiornato)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}