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

    // --- GESTIONE MEMBRI ---

    fun getMembriDelGruppo(gruppoId: String): Flow<List<Amico>> = callbackFlow {
        val registration = db.collection("gruppi").document(gruppoId).collection("membri")
            .addSnapshotListener { snapshot, e ->
                if (e != null) { close(e); return@addSnapshotListener }
                if (snapshot != null) { trySend(snapshot.toObjects<Amico>()) }
            }
        awaitClose { registration.remove() }
    }

    // Recupera un singolo membro dal gruppo (utile per Modifica Amico dentro il gruppo)
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
        } catch (e: Exception) { emptyList() }
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

    // NUOVO: Aggiorna i dati anagrafici (Nome, Posti) di un membro dentro il gruppo
    fun aggiornaAnagraficaMembro(gruppoId: String, amico: Amico) {
        val docRef = db.collection("gruppi").document(gruppoId).collection("membri").document(amico.id)
        // Aggiorniamo solo i campi anagrafici, lasciando stare le statistiche se non le passiamo
        val updates = mapOf(
            "nome" to amico.nome,
            "postiAuto" to amico.postiAuto
        )
        docRef.update(updates)
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

    // --- GESTIONE GRUPPO ---

    fun aggiungiGruppo(gruppo: Gruppo) {
        val userId = auth.currentUser?.uid ?: return
        val idFinale = if (gruppo.id.isEmpty()) UUID.randomUUID().toString() else gruppo.id

        // LOGICA DI ACCESSO CORRETTA:
        // Se stiamo modificando, NON DOBBIAMO PERDERE GLI ALTRI UTENTI!
        // Qui sovrascriviamo solo se è un nuovo inserimento.
        // Ma aspetta: Firestore 'set' sovrascrive tutto. Dobbiamo assicurarci che l'oggetto 'gruppo' passato
        // contenga già la lista completa degli utentiIds.
        // (Questo lo gestiamo nel ViewModel ora, ma qui facciamo un controllo di sicurezza per la creazione).

        val utentiAggiornati = if (gruppo.utentiIds.isEmpty()) {
            // Se la lista è vuota, è un nuovo gruppo -> aggiungo me stesso
            listOf(userId)
        } else {
            // Se la lista c'è già (modifica), mi assicuro di non auto-escludermi per sbaglio
            if (!gruppo.utentiIds.contains(userId)) gruppo.utentiIds + userId else gruppo.utentiIds
        }

        val gruppoDaSalvare = gruppo.copy(id = idFinale, utentiIds = utentiAggiornati)
        db.collection("gruppi").document(idFinale).set(gruppoDaSalvare)
    }

    fun aggiornaGruppo(gruppo: Gruppo) { aggiungiGruppo(gruppo) }

    fun eliminaGruppo(gruppoId: String) {
        uscitaRepository.eliminaTutteUsciteDelGruppo(gruppoId)
        db.collection("gruppi").document(gruppoId).collection("membri").get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) { doc.reference.delete() }
                db.collection("gruppi").document(gruppoId).delete()
            }
    }

    fun lasciaGruppo(gruppoId: String, onResult: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("gruppi").document(gruppoId)
            .update("utentiIds", FieldValue.arrayRemove(userId))
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun generaNuovoId(): String = UUID.randomUUID().toString()

    // --- CONDIVISIONE ---

    fun uniscitiAlGruppo(gruppoId: String, onResult: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
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