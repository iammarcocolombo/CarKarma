package it.col.mar.android.carkarma.presentation.amico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.col.mar.android.carkarma.data.database.AmicoRepository
import it.col.mar.android.carkarma.data.database.GruppoRepository
import it.col.mar.android.carkarma.data.model.Amico
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AmicoViewModel(
    private val amicoRepository: AmicoRepository,
    private val gruppoRepository: GruppoRepository // Aggiunto per modifiche nel gruppo
) : ViewModel() {

    private val _nome = MutableStateFlow("")
    val nome: StateFlow<String> = _nome

    private val _postiAuto = MutableStateFlow("5")
    val postiAuto: StateFlow<String> = _postiAuto

    private var currentAmicoId: String = ""
    private var currentGruppoId: String = "" // Se valorizzato, siamo dentro un gruppo

    fun loadAmico(id: String, gruppoId: String) {
        this.currentAmicoId = id
        this.currentGruppoId = gruppoId

        viewModelScope.launch {
            if (id.isNotEmpty()) {
                val amico = if (gruppoId.isNotEmpty()) {
                    // Carichiamo dal GRUPPO
                    gruppoRepository.getMembro(gruppoId, id)
                } else {
                    // Carichiamo dalla RUBRICA
                    amicoRepository.getAmicoPerId(id)
                }

                amico?.let {
                    _nome.value = it.nome
                    _postiAuto.value = it.postiAuto.toString()
                }
            }
        }
    }

    fun onNomeChange(n: String) { _nome.value = n }

    fun onPostiAutoChange(p: String) {
        if (p.all { it.isDigit() }) {
            _postiAuto.value = p
        }
    }

    fun salvaAmico(onFinito: () -> Unit) {
        val posti = _postiAuto.value.toIntOrNull() ?: 5

        if (currentGruppoId.isNotEmpty()) {
            // --- MODIFICA NEL GRUPPO ---
            // Qui non creiamo nuovi amici, modifichiamo solo quelli esistenti
            if (currentAmicoId.isNotEmpty()) {
                val amicoAggiornato = Amico(
                    id = currentAmicoId,
                    nome = _nome.value,
                    postiAuto = posti
                    // Le statistiche non le tocchiamo qui, il repo saprà cosa fare
                )
                // Chiamiamo la funzione specifica del repo che aggiorna solo anagrafica
                gruppoRepository.aggiornaAnagraficaMembro(currentGruppoId, amicoAggiornato)
            }
        } else {
            // --- MODIFICA/CREAZIONE GLOBALE (RUBRICA) ---
            if (currentAmicoId.isEmpty()) {
                // Nuovo Amico
                val nuovoAmico = Amico(
                    nome = _nome.value,
                    postiAuto = posti
                )
                amicoRepository.aggiungiAmico(nuovoAmico)
            } else {
                // Modifica Esistente
                val vecchioAmico = amicoRepository.getAmicoPerId(currentAmicoId)
                if (vecchioAmico != null) {
                    val amicoAggiornato = vecchioAmico.copy(
                        nome = _nome.value,
                        postiAuto = posti
                    )
                    amicoRepository.aggiungiAmico(amicoAggiornato)
                }
            }
        }
        onFinito()
    }

    fun eliminaAmico(onEliminato: () -> Unit) {
        if (currentAmicoId.isNotEmpty()) {
            if (currentGruppoId.isNotEmpty()) {
                // Elimina dal gruppo (rimuove solo l'istanza)
                gruppoRepository.rimuoviMembroDalGruppo(currentGruppoId, currentAmicoId)
            } else {
                // Elimina dalla rubrica globale
                amicoRepository.rimuoviAmico(currentAmicoId)
            }
        }
        onEliminato()
    }
}