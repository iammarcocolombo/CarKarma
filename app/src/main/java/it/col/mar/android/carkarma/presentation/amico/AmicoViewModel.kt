package it.col.mar.android.carkarma.presentation.amico

import androidx.lifecycle.ViewModel
import it.col.mar.android.carkarma.data.database.AmicoRepository
import it.col.mar.android.carkarma.data.model.Amico
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AmicoViewModel(
    private val amicoRepository: AmicoRepository
) : ViewModel() {

    private val _nome = MutableStateFlow("")
    val nome: StateFlow<String> = _nome

    private val _postiAuto = MutableStateFlow("5") // Default più sensato
    val postiAuto: StateFlow<String> = _postiAuto

    private var currentAmicoId: String = ""

    fun loadAmico(id: String) {
        currentAmicoId = id
        if (id.isNotEmpty()) {
            val amico = amicoRepository.getAmicoPerId(id)
            amico?.let {
                _nome.value = it.nome
                _postiAuto.value = it.postiAuto.toString()
            }
        }
    }

    fun onNomeChange(n: String) { _nome.value = n }

    fun onPostiAutoChange(p: String) {
        // Accetta solo numeri
        if (p.all { it.isDigit() }) {
            _postiAuto.value = p
        }
    }

    fun salvaAmico(onFinito: () -> Unit) {
        val posti = _postiAuto.value.toIntOrNull() ?: 5

        if (currentAmicoId.isEmpty()) {
            // CREAZIONE: Nuovo amico (ID vuoto, lo genererà il Repository/Firebase)
            val nuovoAmico = Amico(
                nome = _nome.value,
                postiAuto = posti,
                // Statistiche iniziali a 0
                uscite = 0,
                guide = 0,
                km = 0
            )
            amicoRepository.aggiungiAmico(nuovoAmico)
        } else {
            // MODIFICA: Recuperiamo il vecchio per non perdere le statistiche!
            val vecchioAmico = amicoRepository.getAmicoPerId(currentAmicoId)

            if (vecchioAmico != null) {
                // Creiamo una copia aggiornata mantenendo ID e Statistiche
                val amicoAggiornato = vecchioAmico.copy(
                    nome = _nome.value,
                    postiAuto = posti
                )

                // Nel nostro repo semplice rimuoviamo e riaggiungiamo
                amicoRepository.rimuoviAmico(currentAmicoId)
                amicoRepository.aggiungiAmico(amicoAggiornato)
            }
        }
        onFinito()
    }

    fun eliminaAmico(onEliminato: () -> Unit) {
        if (currentAmicoId.isNotEmpty()) {
            amicoRepository.rimuoviAmico(currentAmicoId)
        }
        onEliminato()
    }
}