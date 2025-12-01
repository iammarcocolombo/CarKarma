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
        // ASCOLTO IN TEMPO REALE
        // Appena l'utente si logga o i dati cambiano, questa funzione scatta
        auth.addAuthStateListener { firebaseAuth ->
            val userId = firebaseAuth.currentUser?.uid
            if (userId != null) {
                db.collection("users").document(userId).collection("amici")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) return@addSnapshotListener
                        if (snapshot != null) {
                            // Converte automaticamente i documenti JSON in oggetti Amico
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

        // Se è nuovo generiamo ID, altrimenti usiamo quello esistente (per modifiche)
        val idFinale = if (amico.id.isEmpty()) UUID.randomUUID().toString() else amico.id
        val amicoDaSalvare = amico.copy(id = idFinale)

        // Salviamo su Firestore (funziona sia per crea che per aggiorna)
        db.collection("users").document(userId).collection("amici")
            .document(idFinale)
            .set(amicoDaSalvare)
    }

    fun rimuoviAmico(amicoId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("amici")
            .document(amicoId)
            .delete()
    }

    // Aggiorna solo i campi statistici
    fun aggiornaStatisticheAmico(amicoId: String, kmAggiunti: Int, haGuidato: Boolean) {
        val amico = getAmicoPerId(amicoId) ?: return
        val amicoAggiornato = amico.copy(
            uscite = amico.uscite + 1,
            guide = if (haGuidato) amico.guide + 1 else amico.guide,
            km = if (haGuidato) amico.km + kmAggiunti else amico.km
        )
        aggiungiAmico(amicoAggiornato) // "aggiungi" con stesso ID fa un overwrite (aggiornamento)
    }
}