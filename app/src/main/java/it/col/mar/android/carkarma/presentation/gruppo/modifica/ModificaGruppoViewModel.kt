package it.col.mar.android.carkarma.presentation.gruppo.modifica

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import it.col.mar.android.carkarma.data.database.AmicoRepository
import it.col.mar.android.carkarma.data.database.GruppoRepository
import it.col.mar.android.carkarma.data.model.Amico
import it.col.mar.android.carkarma.data.model.Gruppo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ModificaGruppoViewModel(
    private val gruppoRepository: GruppoRepository,
    private val amicoRepository: AmicoRepository
) : ViewModel() {

    // Accesso diretto a Firestore per usare i Batch (più sicuri per salvataggi multipli)
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _nomeGruppo = MutableStateFlow("")
    val nomeGruppo: StateFlow<String> = _nomeGruppo

    // Lista amici globale ("Stampini")
    val amiciDisponibili: StateFlow<List<Amico>> = amicoRepository.amici

    private val _amiciSelezionatiIds = MutableStateFlow<Set<String>>(emptySet())
    val amiciSelezionati: StateFlow<Set<String>> = _amiciSelezionatiIds

    private var currentGruppoId: String = ""
    private var currentUtentiIds: List<String> = emptyList()

    fun loadGruppo(gruppoId: String) {
        this.currentGruppoId = gruppoId
        viewModelScope.launch {
            if (gruppoId.isNotEmpty()) {
                val gruppo = gruppoRepository.getGruppoPerId(gruppoId)
                if (gruppo != null) {
                    _nomeGruppo.value = gruppo.nome
                    currentUtentiIds = gruppo.utentiIds

                    val membriAttuali = gruppoRepository.getMembriDelGruppo(gruppoId).first()
                    _amiciSelezionatiIds.value = membriAttuali.map { it.id }.toSet()
                }
            } else {
                _nomeGruppo.value = ""
                _amiciSelezionatiIds.value = emptySet()
                currentUtentiIds = emptyList()
            }
        }
    }

    fun onNomeGruppoChange(v: String) { _nomeGruppo.value = v }

    fun toggleAmicoSelezionato(amicoId: String) {
        _amiciSelezionatiIds.value = _amiciSelezionatiIds.value.toMutableSet().apply {
            if (contains(amicoId)) remove(amicoId) else add(amicoId)
        }
    }

    fun salvaGruppo(onSalvato: () -> Unit) {
        viewModelScope.launch {
            val idFinale = if (currentGruppoId.isEmpty()) UUID.randomUUID().toString() else currentGruppoId
            val userId = auth.currentUser?.uid ?: return@launch

            // Usiamo un BATCH: Scrittura atomica (tutto o niente)
            val batch = db.batch()

            // 1. Preparazione Gruppo
            val utentiAggiornati = if (currentUtentiIds.contains(userId)) currentUtentiIds else currentUtentiIds + userId
            val gruppo = Gruppo(
                id = idFinale,
                nome = _nomeGruppo.value,
                membriIds = _amiciSelezionatiIds.value.toList(),
                utentiIds = utentiAggiornati
            )
            val gruppoRef = db.collection("gruppi").document(idFinale)
            batch.set(gruppoRef, gruppo)

            // 2. Gestione Membri (Sottocollezione)
            val tuttiStampini = amicoRepository.amici.value
            val idsUI = _amiciSelezionatiIds.value
            val membriRef = gruppoRef.collection("membri")

            if (currentGruppoId.isEmpty()) {
                // NUOVO GRUPPO: Aggiungi tutti i selezionati
                idsUI.forEach { id ->
                    tuttiStampini.find { it.id == id }?.let { template ->
                        val nuovoMembro = template.copy(uscite = 0, guide = 0, km = 0)
                        batch.set(membriRef.document(id), nuovoMembro)
                    }
                }
            } else {
                // MODIFICA: Calcolo differenze per non resettare i km esistenti
                val membriNelDb = gruppoRepository.getMembriDelGruppo(idFinale).first().map { it.id }.toSet()

                // Aggiungi nuovi (reset km)
                idsUI.filter { !membriNelDb.contains(it) }.forEach { id ->
                    tuttiStampini.find { it.id == id }?.let { template ->
                        val nuovoMembro = template.copy(uscite = 0, guide = 0, km = 0)
                        batch.set(membriRef.document(id), nuovoMembro)
                    }
                }
                // Rimuovi deselezionati
                membriNelDb.filter { !idsUI.contains(it) }.forEach { id ->
                    batch.delete(membriRef.document(id))
                }
            }

            try {
                // Eseguiamo tutto in un colpo solo e ASPETTIAMO che finisca
                batch.commit().await()

                // Piccolo ritardo extra per sicurezza UI
                delay(100)
                onSalvato()
            } catch (e: Exception) {
                e.printStackTrace()
                // Qui potresti mostrare un errore con un Toast o stato
            }
        }
    }

    fun eliminaGruppo(onEliminato: () -> Unit) {
        viewModelScope.launch {
            if (currentGruppoId.isNotEmpty()) {
                // Anche qui sarebbe meglio attendere, ma eliminaGruppo nel repo è fire-and-forget.
                // Possiamo chiamare la funzione e aspettare un attimo.
                gruppoRepository.eliminaGruppo(currentGruppoId)
                delay(200)
            }
            onEliminato()
        }
    }
}