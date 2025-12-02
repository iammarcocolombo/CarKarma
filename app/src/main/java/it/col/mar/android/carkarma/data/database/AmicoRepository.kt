package it.col.mar.android.carkarma.data.database

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import it.col.mar.android.carkarma.data.model.Amico
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    fun aggiungiAmico(amico: Amico) {
        val userId = auth.currentUser?.uid ?: return

        val idFinale = if (amico.id.isEmpty()) UUID.randomUUID().toString() else amico.id
        val amicoDaSalvare = amico.copy(id = idFinale)

        db.collection("users").document(userId).collection("amici")
            .document(idFinale)
            .set(amicoDaSalvare)
    }

    // --- NUOVA FUNZIONE: IMPORTA GLI AMICI NELLA RUBRICA PERSONALE ---
    fun importaAmici(listaAmici: List<Amico>) {
        val userId = auth.currentUser?.uid ?: return
        val collectionRef = db.collection("users").document(userId).collection("amici")

        // Usiamo un Batch per scrivere tutto in una volta (più efficiente)
        val batch = db.batch()

        listaAmici.forEach { amico ->
            // Importante: Creiamo uno "stampino" pulito!
            // Azzeriamo le statistiche perché i km fatti nel gruppo dell'amico
            // non devono contare nella mia rubrica generale.
            val template = amico.copy(
                uscite = 0,
                guide = 0,
                km = 0
            )

            // Usiamo 'set' con lo stesso ID.
            // Se l'amico esiste già nella mia rubrica (stesso ID), lo aggiorna (es. se ha cambiato nome).
            // Se non esiste, lo crea.
            val docRef = collectionRef.document(template.id)
            batch.set(docRef, template)
        }

        batch.commit().addOnSuccessListener {
            println("Amici importati con successo nella rubrica personale!")
        }
    }

    fun rimuoviAmico(amicoId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("amici")
            .document(amicoId)
            .delete()
    }

    // Deprecata ma mantenuta per compatibilità se servisse
    fun aggiornaStatisticheAmico(amicoId: String, kmAggiunti: Int, haGuidato: Boolean) {
        // Non fa nulla, ora usiamo i dati nel Gruppo
    }
}