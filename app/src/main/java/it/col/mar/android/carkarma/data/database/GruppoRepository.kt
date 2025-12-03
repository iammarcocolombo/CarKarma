package it.col.mar.android.carkarma.data.database

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import it.col.mar.android.carkarma.data.model.Amico
import it.col.mar.android.carkarma.data.model.Gruppo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
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
        // ASCOLTO FILTRATO PER "UTENTI CON ACCESSO"
        auth.addAuthStateListener { firebaseAuth ->
            val userId = firebaseAuth.currentUser?.uid

            if (userId != null) {
                db.collection("gruppi")
                    .whereArrayContains("utentiIds", userId)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) return@addSnapshotListener
                        if (snapshot != null) {
                            _gruppi.value = snapshot.toObjects<Gruppo>()
                        }
                    }
            } else {
                _gruppi.value = emptyList()
            }
        }
    }

    fun getTuttiIGruppi(): List<Gruppo> = _gruppi.value

    fun getGruppoPerId(id: String): Gruppo? {
        return _gruppi.value.find { it.id == id }
    }

    // --- FUNZIONE DI SINCRONIZZAZIONE (Fix Errore HomeViewModel) ---
    /**
     * Scarica i membri di tutti i gruppi visibili e li importa nella rubrica personale.
     * Utile quando ci si unisce a un nuovo gruppo per avere subito gli amici disponibili.
     */
    suspend fun sincronizzaMembriInRubrica(listaGruppi: List<Gruppo>) {
        if (auth.currentUser == null) return

        listaGruppi.forEach { gruppo ->
            try {
                val snapshot = db.collection("gruppi").document(gruppo.id)
                    .collection("membri").get().await()

                val membriDelGruppo = snapshot.toObjects<Amico>()

                if (membriDelGruppo.isNotEmpty()) {
                    amicoRepository.importaAmici(membriDelGruppo)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- GESTIONE MEMBRI (SOTTOCOLLEZIONE) ---

    fun getMembriDelGruppo(gruppoId: String): Flow<List<Amico>> = callbackFlow {
        val registration = db.collection("gruppi").document(gruppoId).collection("membri")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects<Amico>())
                }
            }
        awaitClose { registration.remove() }
    }

    // Recupera un singolo membro (utile per modifiche mirate)
    suspend fun getMembro(gruppoId: String, amicoId: String): Amico? {
        return try {
            val doc = db.collection("gruppi").document(gruppoId)
                .collection("membri").document(amicoId).get().await()
            doc.toObject(Amico::class.java)
        } catch (e: Exception) { null }
    }

    suspend fun getMembriSnapshot(gruppoId: String): List<Amico> {
        return try {
            val snapshot = db.collection("gruppi").document(gruppoId)
                .collection("membri").get().await()
            snapshot.toObjects<Amico>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun aggiungiMembroAlGruppo(gruppoId: String, amicoTemplate: Amico) {
        val nuovoMembro = amicoTemplate.copy(uscite = 0, guide = 0, km = 0)
        db.collection("gruppi").document(gruppoId)
            .collection("membri").document(nuovoMembro.id)
            .set(nuovoMembro)
    }

    fun rimuoviMembroDalGruppo(gruppoId: String, amicoId: String) {
        db.collection("gruppi").document(gruppoId)
            .collection("membri").document(amicoId)
            .delete()
    }

    fun aggiornaStatisticheMembro(gruppoId: String, amicoId: String, deltaUscite: Int, deltaGuide: Int, deltaKm: Int) {
        val docRef = db.collection("gruppi").document(gruppoId).collection("membri").document(amicoId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val amico = snapshot.toObject(Amico::class.java) ?: return@runTransaction
            val updates = mapOf(
                "uscite" to (amico.uscite + deltaUscite).coerceAtLeast(0),
                "guide" to (amico.guide + deltaGuide).coerceAtLeast(0),
                "km" to (amico.km + deltaKm).coerceAtLeast(0)
            )
            transaction.update(docRef, updates)
        }
    }

    // Aggiorna solo i dati anagrafici (Nome, Posti) nel gruppo senza toccare le statistiche
    fun aggiornaAnagraficaMembro(gruppoId: String, amico: Amico) {
        val docRef = db.collection("gruppi").document(gruppoId).collection("membri").document(amico.id)
        val updates = mapOf(
            "nome" to amico.nome,
            "postiAuto" to amico.postiAuto
        )
        docRef.update(updates)
    }

    // --- GESTIONE GRUPPO ---

    fun aggiungiGruppo(gruppo: Gruppo) {
        val userId = auth.currentUser?.uid ?: return
        val idFinale = if (gruppo.id.isEmpty()) UUID.randomUUID().toString() else gruppo.id

        // Assicuriamoci che chi crea/modifica non si chiuda fuori
        val utentiAggiornati = if (gruppo.utentiIds.contains(userId)) {
            gruppo.utentiIds
        } else {
            gruppo.utentiIds + userId
        }

        val gruppoDaSalvare = gruppo.copy(id = idFinale, utentiIds = utentiAggiornati)
        db.collection("gruppi").document(idFinale).set(gruppoDaSalvare)
    }

    fun aggiornaGruppo(gruppo: Gruppo) {
        aggiungiGruppo(gruppo)
    }

    fun eliminaGruppo(gruppoId: String) {
        uscitaRepository.eliminaTutteUsciteDelGruppo(gruppoId)
        db.collection("gruppi").document(gruppoId).collection("membri").get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    doc.reference.delete()
                }
                db.collection("gruppi").document(gruppoId).delete()
            }
    }

    fun lasciaGruppo(gruppoId: String, onResult: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onResult(false)
            return
        }
        db.collection("gruppi").document(gruppoId)
            .update("utentiIds", FieldValue.arrayRemove(userId))
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun generaNuovoId(): String = UUID.randomUUID().toString()

    // --- CONDIVISIONE ---

    fun uniscitiAlGruppo(gruppoId: String, onResult: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onResult(false)
            return
        }

        db.collection("gruppi").document(gruppoId)
            .update("utentiIds", FieldValue.arrayUnion(userId))
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun rendiUtenteCercabile(uid: String, email: String?, nome: String?) {
        if (email == null) return
        val userMap = mapOf("uid" to uid, "email" to email, "nome" to nome)
        db.collection("public_users").document(uid).set(userMap)
    }
}