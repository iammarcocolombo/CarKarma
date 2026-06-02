package it.col.mar.android.carkarma.data.database

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import it.col.mar.android.carkarma.data.model.Amico
import it.col.mar.android.carkarma.data.model.Gruppo
import it.col.mar.android.carkarma.domain.repository.AmicoRepository
import it.col.mar.android.carkarma.domain.repository.GruppoRepository
import it.col.mar.android.carkarma.domain.repository.UscitaRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class GruppoRepositoryImpl(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage,
    private val amicoRepository: AmicoRepository,
    private val uscitaRepository: UscitaRepository
) : GruppoRepository {

    private val _gruppi = MutableStateFlow<List<Gruppo>>(emptyList())
    override val gruppi: StateFlow<List<Gruppo>> = _gruppi.asStateFlow()

    private val _isDataLoaded = MutableStateFlow(false)
    override val isDataLoaded: StateFlow<Boolean> = _isDataLoaded.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val userId = firebaseAuth.currentUser?.uid

            if (userId != null) {
                db.collection("gruppi")
                    .whereArrayContains("utentiIds", userId)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) return@addSnapshotListener
                        if (snapshot != null) {
                            _gruppi.value = snapshot.toObjects(Gruppo::class.java)
                            _isDataLoaded.value = true
                        }
                    }
            } else {
                _gruppi.value = emptyList()
                _isDataLoaded.value = true
            }
        }
    }

    override suspend fun sincronizzaMembriInRubrica(listaGruppi: List<Gruppo>) {
        if (auth.currentUser == null) return
        listaGruppi.forEach { gruppo ->
            try {
                val snapshot = db.collection("gruppi").document(gruppo.id)
                    .collection("membri").get().await()
                val membriDelGruppo = snapshot.toObjects(Amico::class.java)
                if (membriDelGruppo.isNotEmpty()) {
                    amicoRepository.importaAmici(membriDelGruppo)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    override suspend fun uploadImmagineGruppo(gruppoId: String, uriImmagine: Uri): String? {
        return try {
            val storageRef = storage.reference.child("immagini_gruppi/$gruppoId.jpg")
            storageRef.putFile(uriImmagine).await()
            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    override fun getMembriDelGruppo(gruppoId: String): Flow<List<Amico>> = callbackFlow {
        val registration = db.collection("gruppi").document(gruppoId).collection("membri")
            .addSnapshotListener { snapshot, e ->
                if (e != null) { close(e); return@addSnapshotListener }
                if (snapshot != null) { trySend(snapshot.toObjects(Amico::class.java)) }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun getMembriSnapshot(gruppoId: String): List<Amico> {
        return try {
            val snapshot = db.collection("gruppi").document(gruppoId)
                .collection("membri").get().await()
            snapshot.toObjects(Amico::class.java)
        } catch (_: Exception) { emptyList() }
    }

    override suspend fun getMembro(gruppoId: String, amicoId: String): Amico? {
        return try {
            val doc = db.collection("gruppi").document(gruppoId)
                .collection("membri").document(amicoId).get().await()
            doc.toObject(Amico::class.java)
        } catch (_: Exception) { null }
    }

    override fun aggiungiMembroAlGruppo(gruppoId: String, amicoTemplate: Amico) {
        val nuovoMembro = amicoTemplate.copy(uscite = 0, guide = 0, km = 0, karma = 0.0)
        db.collection("gruppi").document(gruppoId).collection("membri").document(nuovoMembro.id).set(nuovoMembro)
    }

    override fun rimuoviMembroDalGruppo(gruppoId: String, amicoId: String) {
        db.collection("gruppi").document(gruppoId).collection("membri").document(amicoId).delete()
    }

    override fun aggiornaStatisticheMembro(
        gruppoId: String,
        amicoId: String,
        deltaUscite: Int,
        deltaGuide: Int,
        deltaKm: Int,
        deltaKarma: Double
    ) {
        val docRef = db.collection("gruppi").document(gruppoId).collection("membri").document(amicoId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val amico = snapshot.toObject(Amico::class.java) ?: return@runTransaction

            val karmaAttuale = if (amico.karma != 0.0) amico.karma else amico.km.toDouble()

            val updates = mapOf(
                "uscite" to (amico.uscite + deltaUscite).coerceAtLeast(0),
                "guide" to (amico.guide + deltaGuide).coerceAtLeast(0),
                "km" to (amico.km + deltaKm).coerceAtLeast(0),
                "karma" to (karmaAttuale + deltaKarma).coerceAtLeast(0.0)
            )
            transaction.update(docRef, updates)
        }
    }

    override fun aggiornaAnagraficaMembro(gruppoId: String, amico: Amico) {
        val docRef = db.collection("gruppi").document(gruppoId).collection("membri").document(amico.id)
        val updates = mapOf(
            "nome" to amico.nome,
            "postiAuto" to amico.postiAuto,
            "tipoCarburante" to amico.tipoCarburante,
            "consumoMedio" to amico.consumoMedio
        )
        docRef.update(updates)
    }

    override fun getTuttiIGruppi(): List<Gruppo> = _gruppi.value
    override fun getGruppoPerId(id: String): Gruppo? = _gruppi.value.find { it.id == id }

    override fun aggiungiGruppo(gruppo: Gruppo) {
        val userId = auth.currentUser?.uid ?: return
        val idFinale = if (gruppo.id.isEmpty()) UUID.randomUUID().toString() else gruppo.id
        val utentiAggiornati = if (gruppo.utentiIds.contains(userId)) gruppo.utentiIds else gruppo.utentiIds + userId
        val gruppoDaSalvare = gruppo.copy(id = idFinale, utentiIds = utentiAggiornati)
        db.collection("gruppi").document(idFinale).set(gruppoDaSalvare)
    }

    override fun aggiornaGruppo(gruppo: Gruppo) { aggiungiGruppo(gruppo) }

    override fun eliminaGruppo(gruppoId: String) {
        uscitaRepository.eliminaTutteUsciteDelGruppo(gruppoId)
        db.collection("gruppi").document(gruppoId).collection("membri").get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) { doc.reference.delete() }
                db.collection("gruppi").document(gruppoId).delete()
            }
    }

    override fun lasciaGruppo(gruppoId: String, onResult: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) { onResult(false); return }
        db.collection("gruppi").document(gruppoId).update("utentiIds", FieldValue.arrayRemove(userId))
            .addOnSuccessListener { onResult(true) }.addOnFailureListener { onResult(false) }
    }

    override fun generaNuovoId(): String = UUID.randomUUID().toString()

    override fun uniscitiAlGruppo(gruppoId: String, onResult: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) { onResult(false); return }
        db.collection("gruppi").document(gruppoId).update("utentiIds", FieldValue.arrayUnion(userId))
            .addOnSuccessListener { onResult(true) }.addOnFailureListener { onResult(false) }
    }

    override fun rendiUtenteCercabile(uid: String, email: String?, nome: String?) {
        if (email == null) return
        val userMap = mapOf("uid" to uid, "email" to email, "nome" to nome)
        db.collection("public_users").document(uid).set(userMap)
    }

    override suspend fun eliminaDatiUtentePubblico() {
        val uid = auth.currentUser?.uid ?: return
        try { db.collection("public_users").document(uid).delete().await() } catch (e: Exception) { e.printStackTrace() }
    }

    override suspend fun rimuoviUtenteDaTuttiIGruppi() {
        val uid = auth.currentUser?.uid ?: return
        try {
            val snapshot = db.collection("gruppi").whereArrayContains("utentiIds", uid).get().await()
            val batch = db.batch()
            for (doc in snapshot.documents) {
                batch.update(doc.reference, "utentiIds", FieldValue.arrayRemove(uid))
            }
            batch.commit().await()
        } catch (e: Exception) { e.printStackTrace() }
    }
}