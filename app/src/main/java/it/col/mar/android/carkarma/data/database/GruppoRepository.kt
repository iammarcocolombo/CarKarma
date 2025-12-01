package it.col.mar.android.carkarma.data.database

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import it.col.mar.android.carkarma.data.model.Gruppo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class GruppoRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val amicoRepository: AmicoRepository
) {
    private val _gruppi = MutableStateFlow<List<Gruppo>>(emptyList())
    val gruppi: StateFlow<List<Gruppo>> = _gruppi.asStateFlow()

    init {
        // Ascoltiamo la collezione "gruppi" globale in tempo reale
        // Appena aggiungi un gruppo, questa lista si aggiorna da sola
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

    fun aggiungiGruppo(gruppo: Gruppo) {
        // Se l'ID è vuoto (nuovo gruppo), ne generiamo uno univoco
        val idFinale = if (gruppo.id.isEmpty()) UUID.randomUUID().toString() else gruppo.id
        val gruppoDaSalvare = gruppo.copy(id = idFinale)

        // Salviamo su Firebase (collezione "gruppi", documento con ID specifico)
        db.collection("gruppi").document(idFinale).set(gruppoDaSalvare)
    }

    // Alias per aggiorna (in Firestore add e update sono la stessa operazione "set")
    fun aggiornaGruppo(gruppo: Gruppo) {
        aggiungiGruppo(gruppo)
    }

    fun eliminaGruppo(gruppoId: String) {
        db.collection("gruppi").document(gruppoId).delete()
    }

    // Metodo helper per generare ID se servisse (ma ora lo facciamo in aggiungiGruppo)
    fun generaNuovoId(): String {
        return UUID.randomUUID().toString()
    }
}