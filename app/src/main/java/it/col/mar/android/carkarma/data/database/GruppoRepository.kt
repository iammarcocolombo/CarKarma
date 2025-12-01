package it.col.mar.android.carkarma.data.database

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObjects
import it.col.mar.android.carkarma.data.model.Amico
import it.col.mar.android.carkarma.data.model.Gruppo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

class GruppoRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val amicoRepository: AmicoRepository,
    private val uscitaRepository: UscitaRepository
) {
    private val _gruppi = MutableStateFlow<List<Gruppo>>(emptyList())
    val gruppi: StateFlow<List<Gruppo>> = _gruppi.asStateFlow()

    init {
        // Ascolto gruppi globali
        db.collection("gruppi")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    _gruppi.value = snapshot.toObjects<Gruppo>()
                }
            }
    }

    fun getTuttiIGruppi(): List<Gruppo> = _gruppi.value

    fun getGruppoPerId(id: String): Gruppo? {
        return _gruppi.value.find { it.id == id }
    }

    // --- NUOVA GESTIONE MEMBRI (SOTTOCOLLEZIONE) ---

    /**
     * Restituisce un Flow in tempo reale dei membri di uno specifico gruppo.
     * Questi sono le "istanze" con i km specifici per questo gruppo.
     */
    fun getMembriDelGruppo(gruppoId: String): Flow<List<Amico>> = callbackFlow {
        val registration = db.collection("gruppi").document(gruppoId).collection("membri")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val membri = snapshot.toObjects<Amico>()
                    trySend(membri)
                }
            }
        awaitClose { registration.remove() }
    }

    /**
     * Prende un amico "stampino" dalla rubrica e ne crea una copia nel gruppo.
     * I km vengono resettati a 0 per il nuovo contesto.
     */
    fun aggiungiMembroAlGruppo(gruppoId: String, amicoTemplate: Amico) {
        // Creiamo la "nuova istanza" per questo gruppo
        // Manteniamo lo stesso ID per comodità di riferimento, oppure ne generiamo uno nuovo se preferisci duplicati
        // Qui usiamo lo stesso ID così "Marco" è sempre "Marco", ma i dati km sono separati.
        val nuovoMembro = amicoTemplate.copy(
            uscite = 0,
            guide = 0,
            km = 0 // Reset statistiche per il nuovo gruppo
        )

        db.collection("gruppi").document(gruppoId)
            .collection("membri")
            .document(nuovoMembro.id)
            .set(nuovoMembro)

        // Aggiorniamo anche la lista ids nel padre per riferimento veloce (opzionale ma utile)
        // Nota: in una app complessa si userebbe FieldValue.arrayUnion
    }

    fun rimuoviMembroDalGruppo(gruppoId: String, amicoId: String) {
        db.collection("gruppi").document(gruppoId)
            .collection("membri")
            .document(amicoId)
            .delete()
    }

    // --- AGGIORNAMENTO STATISTICHE DEL GRUPPO ---
    // Questa funzione va a modificare solo l'istanza dell'amico DENTRO questo gruppo
    fun aggiornaStatisticheMembro(gruppoId: String, amicoId: String, kmAggiunti: Int, haGuidato: Boolean) {
        val docRef = db.collection("gruppi").document(gruppoId).collection("membri").document(amicoId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val amico = snapshot.toObject(Amico::class.java) ?: return@runTransaction

            val updates = mapOf(
                "uscite" to amico.uscite + 1,
                "guide" to if (haGuidato) amico.guide + 1 else amico.guide,
                "km" to if (haGuidato) amico.km + kmAggiunti else amico.km
            )
            transaction.update(docRef, updates)
        }
    }

    // --- GESTIONE GRUPPO ---

    fun aggiungiGruppo(gruppo: Gruppo) {
        val idFinale = if (gruppo.id.isEmpty()) UUID.randomUUID().toString() else gruppo.id
        val gruppoDaSalvare = gruppo.copy(id = idFinale)
        db.collection("gruppi").document(idFinale).set(gruppoDaSalvare)
    }

    fun aggiornaGruppo(gruppo: Gruppo) {
        aggiungiGruppo(gruppo)
    }

    fun eliminaGruppo(gruppoId: String) {
        uscitaRepository.eliminaUscitePerGruppo(gruppoId)

        // Dobbiamo eliminare manualmente la sottocollezione membri
        // (Firestore non cancella le sottocollezioni in automatico quando cancelli il padre)
        db.collection("gruppi").document(gruppoId).collection("membri").get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    doc.reference.delete()
                }
                // Infine eliminiamo il gruppo
                db.collection("gruppi").document(gruppoId).delete()
            }
    }
}