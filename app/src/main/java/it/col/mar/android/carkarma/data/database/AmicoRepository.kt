package it.col.mar.android.carkarma.data.database

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import it.col.mar.android.carkarma.data.model.Amico
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AmicoRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val _amici = MutableStateFlow<List<Amico>>(emptyList())
    val amici: StateFlow<List<Amico>> = _amici.asStateFlow()

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

    fun getTuttiGliAmici(): List<Amico> = _amici.value

    fun getAmicoPerId(id: String): Amico? {
        return _amici.value.find { it.id == id }
    }

    // MODIFICA: Ora è 'suspend' e aspetta (.await())
    suspend fun aggiungiAmico(amico: Amico) {
        val userId = auth.currentUser?.uid ?: return

        val idFinale = if (amico.id.isEmpty()) UUID.randomUUID().toString() else amico.id
        val amicoDaSalvare = amico.copy(id = idFinale)

        try {
            db.collection("users").document(userId).collection("amici")
                .document(idFinale)
                .set(amicoDaSalvare)
                .await() // Aspetta che Firebase confermi!
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun importaAmici(listaAmici: List<Amico>) {
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

    suspend fun rimuoviAmico(amicoId: String) {
        val userId = auth.currentUser?.uid ?: return
        try {
            db.collection("users").document(userId).collection("amici")
                .document(amicoId)
                .delete()
                .await() // Aspetta la cancellazione
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}