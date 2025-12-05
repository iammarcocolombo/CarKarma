package it.col.mar.android.carkarma.presentation.gruppo.modifica

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
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

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _nomeGruppo = MutableStateFlow("")
    val nomeGruppo: StateFlow<String> = _nomeGruppo

    // NUOVO STATO: L'indice dell'avatar selezionato
    private val _selectedAvatarIndex = MutableStateFlow(0)
    val selectedAvatarIndex: StateFlow<Int> = _selectedAvatarIndex

    // Lista amici globale ("Stampini") per la selezione
    val amiciDisponibili: StateFlow<List<Amico>> = amicoRepository.amici

    // Set degli ID degli amici selezionati nella UI
    private val _amiciSelezionatiIds = MutableStateFlow<Set<String>>(emptySet())
    val amiciSelezionati: StateFlow<Set<String>> = _amiciSelezionatiIds

    private var currentGruppoId: String = ""
    // Salviamo la lista degli utenti reali (account Google) che hanno accesso al gruppo per non perderla
    private var currentUtentiIds: List<String> = emptyList()

    fun loadGruppo(gruppoId: String) {
        this.currentGruppoId = gruppoId
        viewModelScope.launch {
            if (gruppoId.isNotEmpty()) {
                val gruppo = gruppoRepository.getGruppoPerId(gruppoId)
                if (gruppo != null) {
                    _nomeGruppo.value = gruppo.nome
                    _selectedAvatarIndex.value = gruppo.avatarIndex // Carichiamo l'avatar salvato
                    currentUtentiIds = gruppo.utentiIds

                    // Carichiamo i membri che sono GIA' nel gruppo per pre-spuntare le checkbox
                    val membriAttuali = gruppoRepository.getMembriDelGruppo(gruppoId).first()
                    _amiciSelezionatiIds.value = membriAttuali.map { it.id }.toSet()
                }
            } else {
                // Nuovo gruppo: tutto vuoto
                _nomeGruppo.value = ""
                _selectedAvatarIndex.value = 0
                _amiciSelezionatiIds.value = emptySet()
                currentUtentiIds = emptyList()
            }
        }
    }

    fun onNomeGruppoChange(v: String) { _nomeGruppo.value = v }

    fun onAvatarSelected(index: Int) { _selectedAvatarIndex.value = index }

    fun toggleAmicoSelezionato(amicoId: String) {
        _amiciSelezionatiIds.value = _amiciSelezionatiIds.value.toMutableSet().apply {
            if (contains(amicoId)) remove(amicoId) else add(amicoId)
        }
    }

    fun salvaGruppo(onSalvato: () -> Unit) {
        viewModelScope.launch {
            val idFinale = if (currentGruppoId.isEmpty()) UUID.randomUUID().toString() else currentGruppoId
            val userId = auth.currentUser?.uid ?: return@launch

            // Usiamo un BATCH: Scrittura atomica (tutto o niente) per sicurezza
            val batch = db.batch()

            // 1. Preparazione Gruppo
            // Se è nuovo, aggiungo me stesso. Se esiste, mantengo la lista esistente + me stesso (se mancassi)
            val utentiAggiornati = if (currentUtentiIds.contains(userId)) currentUtentiIds else currentUtentiIds + userId

            val gruppo = Gruppo(
                id = idFinale,
                nome = _nomeGruppo.value,
                membriIds = _amiciSelezionatiIds.value.toList(),
                utentiIds = utentiAggiornati,
                avatarIndex = _selectedAvatarIndex.value // Salviamo l'indice scelto
            )

            val gruppoRef = db.collection("gruppi").document(idFinale)
            batch.set(gruppoRef, gruppo)

            // 2. Gestione Membri (Sottocollezione)
            val tuttiStampini = amicoRepository.amici.value
            val idsUI = _amiciSelezionatiIds.value
            val membriRef = gruppoRef.collection("membri")

            if (currentGruppoId.isEmpty()) {
                // CASO A: NUOVO GRUPPO -> Aggiungi tutti i selezionati come nuove istanze (km 0)
                idsUI.forEach { id ->
                    tuttiStampini.find { it.id == id }?.let { template ->
                        val nuovoMembro = template.copy(uscite = 0, guide = 0, km = 0)
                        batch.set(membriRef.document(id), nuovoMembro)
                    }
                }
            } else {
                // CASO B: MODIFICA -> Calcolo differenze per non resettare i km di chi c'è già
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
                // Chi c'è in entrambi non viene toccato, preservando i km
            }

            try {
                // Eseguiamo tutto in un colpo solo e ASPETTIAMO che finisca
                batch.commit().await()

                // Piccolo ritardo extra per dare tempo alla UI di aggiornarsi
                delay(100)
                onSalvato()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun eliminaGruppo(onEliminato: () -> Unit) {
        viewModelScope.launch {
            if (currentGruppoId.isNotEmpty()) {
                gruppoRepository.eliminaGruppo(currentGruppoId)
                delay(200)
            }
            onEliminato()
        }
    }
}