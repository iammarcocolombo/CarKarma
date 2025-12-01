package it.col.mar.android.carkarma.data.database

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import it.col.mar.android.carkarma.data.model.Uscita
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class UscitaRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val _uscite = MutableStateFlow<List<Uscita>>(emptyList())
    val uscite: StateFlow<List<Uscita>> = _uscite.asStateFlow()

    init {
        // Ascolto real-time della collezione "uscite"
        db.collection("uscite")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    _uscite.value = snapshot.toObjects<Uscita>()
                }
            }
    }

    fun getTutteLeUscite(): List<Uscita> = _uscite.value

    fun getUscitePerGruppo(gruppoId: String): List<Uscita> {
        return _uscite.value.filter { it.gruppoId == gruppoId }
    }

    fun getUscitaPerId(id: String): Uscita? {
        return _uscite.value.find { it.id == id }
    }

    fun aggiungiUscita(uscita: Uscita) {
        val idFinale = if (uscita.id.isEmpty()) UUID.randomUUID().toString() else uscita.id
        val uscitaDaSalvare = uscita.copy(id = idFinale)
        db.collection("uscite").document(idFinale).set(uscitaDaSalvare)
    }

    fun aggiornaUscita(uscita: Uscita) {
        aggiungiUscita(uscita)
    }

    fun eliminaUscita(uscitaId: String) {
        db.collection("uscite").document(uscitaId).delete()
    }

    // Funzione chiave per la tua richiesta
    fun eliminaUscitePerGruppo(gruppoId: String) {
        // Troviamo tutte le uscite di questo gruppo nella nostra lista locale
        val usciteDelGruppo = _uscite.value.filter { it.gruppoId == gruppoId }

        // Le eliminiamo una per una da Firebase
        for (uscita in usciteDelGruppo) {
            eliminaUscita(uscita.id)
        }
    }
}