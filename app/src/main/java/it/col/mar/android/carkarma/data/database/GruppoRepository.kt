package it.col.mar.android.carkarma.data.database

import com.google.firebase.auth.FirebaseAuth
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
        // Scarichiamo solo i gruppi dove l'utente corrente ha i permessi (è in utentiIds)
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
                // Se l'utente fa logout, puliamo la lista per sicurezza
                _gruppi.value = emptyList()
            }
        }
    }

    fun getTuttiIGruppi(): List<Gruppo> = _gruppi.value

    fun getGruppoPerId(id: String): Gruppo? {
        return _gruppi.value.find { it.id == id }
    }

    // --- GESTIONE MEMBRI (SOTTOCOLLEZIONE "ISTANZE") ---

    /**
     * Restituisce un Flow in tempo reale dei membri (amici) di uno specifico gruppo.
     */
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

    /**
     * Aggiunge un amico al gruppo creando una copia "pulita" (km azzerati).
     */
    fun aggiungiMembroAlGruppo(gruppoId: String, amicoTemplate: Amico) {
        val nuovoMembro = amicoTemplate.copy(
            uscite = 0,
            guide = 0,
            km = 0 // Reset per il nuovo contesto
        )
        db.collection("gruppi").document(gruppoId)
            .collection("membri").document(nuovoMembro.id)
            .set(nuovoMembro)
    }

    fun rimuoviMembroDalGruppo(gruppoId: String, amicoId: String) {
        db.collection("gruppi").document(gruppoId)
            .collection("membri").document(amicoId)
            .delete()
    }

    /**
     * Aggiorna le statistiche di un membro specifico usando una transazione atomica.
     * Supporta valori negativi per correzioni/rollback.
     */
    fun aggiornaStatisticheMembro(gruppoId: String, amicoId: String, deltaUscite: Int, deltaGuide: Int, deltaKm: Int) {
        val docRef = db.collection("gruppi").document(gruppoId).collection("membri").document(amicoId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val amico = snapshot.toObject(Amico::class.java) ?: return@runTransaction

            // Calcoliamo i nuovi valori assicurandoci che non vadano sotto zero
            val nuoviUscite = (amico.uscite + deltaUscite).coerceAtLeast(0)
            val nuoveGuide = (amico.guide + deltaGuide).coerceAtLeast(0)
            val nuoviKm = (amico.km + deltaKm).coerceAtLeast(0)

            val updates = mapOf(
                "uscite" to nuoviUscite,
                "guide" to nuoveGuide,
                "km" to nuoviKm
            )
            transaction.update(docRef, updates)
        }
    }

    // --- GESTIONE GRUPPO (CREAZIONE E ACCESSO) ---

    fun aggiungiGruppo(gruppo: Gruppo) {
        val userId = auth.currentUser?.uid ?: return

        val idFinale = if (gruppo.id.isEmpty()) UUID.randomUUID().toString() else gruppo.id

        // LOGICA DI ACCESSO:
        // Chi crea il gruppo (io) deve avere subito l'accesso, quindi mi aggiungo a 'utentiIds'.
        // Questo garantisce che la query nel blocco init trovi il gruppo appena creato.
        val utentiAggiornati = if (gruppo.utentiIds.contains(userId)) {
            gruppo.utentiIds
        } else {
            gruppo.utentiIds + userId
        }

        val gruppoDaSalvare = gruppo.copy(
            id = idFinale,
            utentiIds = utentiAggiornati
            // Nota: membriIds rimane quello passato dal ViewModel (la lista degli ID degli amici selezionati)
        )

        db.collection("gruppi").document(idFinale).set(gruppoDaSalvare)
    }

    fun aggiornaGruppo(gruppo: Gruppo) {
        aggiungiGruppo(gruppo)
    }

    fun eliminaGruppo(gruppoId: String) {
        // 1. Elimina tutte le uscite associate (tramite UscitaRepository)
        uscitaRepository.eliminaTutteUsciteDelGruppo(gruppoId)

        // 2. Elimina tutti i membri dalla sottocollezione
        db.collection("gruppi").document(gruppoId).collection("membri").get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    doc.reference.delete()
                }
                // 3. Infine, elimina il documento del gruppo stesso
                db.collection("gruppi").document(gruppoId).delete()
            }
    }

    fun generaNuovoId(): String = UUID.randomUUID().toString()
}