package it.col.mar.android.carkarma.presentation.amico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.col.mar.android.carkarma.data.database.AmicoRepository
import it.col.mar.android.carkarma.data.database.GruppoRepository
import it.col.mar.android.carkarma.data.model.Amico
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AmicoViewModel(
    private val amicoRepository: AmicoRepository,
    private val gruppoRepository: GruppoRepository // Necessario per modifiche nel contesto del gruppo
) : ViewModel() {

    private val _nome = MutableStateFlow("")
    val nome: StateFlow<String> = _nome

    private val _postiAuto = MutableStateFlow("5")
    val postiAuto: StateFlow<String> = _postiAuto

    private var currentAmicoId: String = ""
    private var currentGruppoId: String = "" // Se valorizzato, siamo dentro un gruppo

    // FIX: Ora accetta 2 parametri per supportare la modifica nel gruppo
    fun loadAmico(id: String, gruppoId: String) {
        this.currentAmicoId = id
        this.currentGruppoId = gruppoId

        viewModelScope.launch {
            if (id.isNotEmpty()) {
                // Logica bimodale: Carichiamo dal Gruppo o dalla Rubrica?
                val amico = if (gruppoId.isNotEmpty()) {
                    gruppoRepository.getMembro(gruppoId, id)
                } else {
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

        viewModelScope.launch {
            try {
                if (currentGruppoId.isNotEmpty()) {
                    // --- MODIFICA NEL GRUPPO (ISTANZA) ---
                    if (currentAmicoId.isNotEmpty()) {
                        val amicoAggiornato = Amico(
                            id = currentAmicoId,
                            nome = _nome.value,
                            postiAuto = posti
                        )
                        gruppoRepository.aggiornaAnagraficaMembro(currentGruppoId, amicoAggiornato)
                    }
                } else {
                    // --- MODIFICA/CREAZIONE GLOBALE (RUBRICA) ---
                    val amicoDaSalvare = if (currentAmicoId.isEmpty()) {
                        Amico(nome = _nome.value, postiAuto = posti)
                    } else {
                        amicoRepository.getAmicoPerId(currentAmicoId)?.copy(
                            nome = _nome.value,
                            postiAuto = posti
                        ) ?: Amico(id = currentAmicoId, nome = _nome.value, postiAuto = posti)
                    }

                    amicoRepository.aggiungiAmico(amicoDaSalvare)
                }

                delay(200) // Piccolo ritardo per sync
                onFinito()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun eliminaAmico(onEliminato: () -> Unit) {
        viewModelScope.launch {
            try {
                if (currentAmicoId.isNotEmpty()) {
                    if (currentGruppoId.isNotEmpty()) {
                        gruppoRepository.rimuoviMembroDalGruppo(currentGruppoId, currentAmicoId)
                    } else {
                        amicoRepository.rimuoviAmico(currentAmicoId)
                    }
                }
                delay(200)
                onEliminato()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}