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
    private val gruppoRepository: GruppoRepository
) : ViewModel() {

    private val _nome = MutableStateFlow("")
    val nome: StateFlow<String> = _nome

    private val _postiAuto = MutableStateFlow("5")
    val postiAuto: StateFlow<String> = _postiAuto

    // NUOVI STATI PER AUTO
    private val _tipoCarburante = MutableStateFlow("Benzina")
    val tipoCarburante: StateFlow<String> = _tipoCarburante

    private val _consumoMedio = MutableStateFlow("") // Stringa per gestire l'input (es. "6,5")
    val consumoMedio: StateFlow<String> = _consumoMedio

    private var currentAmicoId: String = ""
    private var currentGruppoId: String = ""

    fun loadAmico(id: String, gruppoId: String) {
        this.currentAmicoId = id
        this.currentGruppoId = gruppoId

        viewModelScope.launch {
            if (id.isNotEmpty()) {
                // Recuperiamo l'amico dal posto giusto (Gruppo o Rubrica)
                val amico = if (gruppoId.isNotEmpty()) {
                    gruppoRepository.getMembro(gruppoId, id)
                } else {
                    amicoRepository.getAmicoPerId(id)
                }

                amico?.let {
                    _nome.value = it.nome
                    _postiAuto.value = it.postiAuto.toString()
                    _tipoCarburante.value = it.tipoCarburante
                    // Convertiamo il double in stringa solo se ha senso
                    _consumoMedio.value = if (it.consumoMedio > 0.0) it.consumoMedio.toString() else ""
                }
            } else {
                // Reset per nuovo inserimento
                _nome.value = ""
                _postiAuto.value = "5"
                _tipoCarburante.value = "Benzina"
                _consumoMedio.value = ""
            }
        }
    }

    fun onNomeChange(n: String) { _nome.value = n }

    fun onPostiAutoChange(p: String) {
        if (p.all { it.isDigit() }) {
            _postiAuto.value = p
        }
    }

    fun onTipoCarburanteChange(t: String) { _tipoCarburante.value = t }

    fun onConsumoChange(c: String) {
        // Accettiamo solo cifre, punto e virgola per evitare crash
        if (c.all { it.isDigit() || it == '.' || it == ',' }) {
            _consumoMedio.value = c
        }
    }

    fun salvaAmico(onFinito: () -> Unit) {
        val posti = _postiAuto.value.toIntOrNull() ?: 5

        // Conversione sicura: sostituisce la virgola italiana con il punto decimale
        val consumo = _consumoMedio.value.replace(',', '.').toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            try {
                if (currentGruppoId.isNotEmpty()) {
                    // --- MODIFICA MEMBRO DEL GRUPPO ---
                    if (currentAmicoId.isNotEmpty()) {
                        val membroEsistente = gruppoRepository.getMembro(currentGruppoId, currentAmicoId)
                        if (membroEsistente != null) {
                            // Creiamo l'oggetto aggiornato mantenendo le statistiche (uscite, guide, km)
                            // ma aggiornando i dati anagrafici e dell'auto
                            val amicoAggiornato = membroEsistente.copy(
                                nome = _nome.value,
                                postiAuto = posti,
                                tipoCarburante = _tipoCarburante.value,
                                consumoMedio = consumo
                            )
                            // Usiamo aggiungiMembroAlGruppo che fa un .set() sovrascrivendo i dati
                            // Nota: Dato che 'amicoAggiornato' ha già le statistiche vecchie dentro, non le perdiamo.
                            gruppoRepository.aggiungiMembroAlGruppo(currentGruppoId, amicoAggiornato)
                        }
                    }
                } else {
                    // --- MODIFICA/CREAZIONE RUBRICA GLOBALE ---
                    val amicoBase = if (currentAmicoId.isNotEmpty()) {
                        amicoRepository.getAmicoPerId(currentAmicoId)
                    } else null

                    val amicoDaSalvare = if (amicoBase != null) {
                        // Modifica esistente
                        amicoBase.copy(
                            nome = _nome.value,
                            postiAuto = posti,
                            tipoCarburante = _tipoCarburante.value,
                            consumoMedio = consumo
                        )
                    } else {
                        // Nuovo amico
                        Amico(
                            id = if(currentAmicoId.isNotEmpty()) currentAmicoId else java.util.UUID.randomUUID().toString(),
                            nome = _nome.value,
                            postiAuto = posti,
                            // I nuovi campi hanno valori di default nel costruttore, ma li settiamo esplicitamente
                            tipoCarburante = _tipoCarburante.value,
                            consumoMedio = consumo
                        )
                    }

                    amicoRepository.aggiungiAmico(amicoDaSalvare)
                }

                delay(200) // Piccolo ritardo per dare tempo a Firestore
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