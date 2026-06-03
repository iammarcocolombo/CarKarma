package it.col.mar.android.carkarma.presentation.amico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.col.mar.android.carkarma.domain.repository.AmicoRepository
import it.col.mar.android.carkarma.domain.repository.GruppoRepository
import it.col.mar.android.carkarma.data.model.Amico
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AmicoViewModel(
    private val amicoRepository: AmicoRepository,
    private val gruppoRepository: GruppoRepository
) : ViewModel() {

    private val _nome = MutableStateFlow("")
    val nome: StateFlow<String> = _nome

    private val _postiAuto = MutableStateFlow("5")
    val postiAuto: StateFlow<String> = _postiAuto

    private val _tipoCarburante = MutableStateFlow("Benzina")
    val tipoCarburante: StateFlow<String> = _tipoCarburante

    private val _consumoMedio = MutableStateFlow("")
    val consumoMedio: StateFlow<String> = _consumoMedio

    private var currentAmicoId: String = ""
    private var currentGruppoId: String = ""

    fun loadAmico(id: String, gruppoId: String) {
        val idPulito = if (id == "{amicoId}" || id == "null" || id.isBlank()) "" else id
        val gruppoIdPulito = if (gruppoId == "{gruppoId}" || gruppoId == "null" || gruppoId.isBlank()) "" else gruppoId

        this.currentAmicoId = idPulito
        this.currentGruppoId = gruppoIdPulito

        viewModelScope.launch {
            if (idPulito.isNotEmpty()) {
                val amico = if (gruppoIdPulito.isNotEmpty()) {
                    gruppoRepository.getMembro(gruppoIdPulito, idPulito) ?: amicoRepository.getAmicoPerId(idPulito)
                } else {
                    amicoRepository.getAmicoPerId(idPulito)
                }

                amico?.let {
                    _nome.value = it.nome
                    _postiAuto.value = it.postiAuto.toString()
                    _tipoCarburante.value = it.tipoCarburante
                    _consumoMedio.value = if (it.consumoMedio > 0.0) it.consumoMedio.toString() else ""
                }
            } else {
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
        if (c.all { it.isDigit() || it == '.' || it == ',' }) {
            _consumoMedio.value = c
        }
    }

    fun salvaAmico(onFinito: () -> Unit) {
        val posti = _postiAuto.value.toIntOrNull() ?: 5
        val consumo = _consumoMedio.value.replace(',', '.').toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            try {
                if (currentGruppoId.isNotEmpty()) {
                    if (currentAmicoId.isNotEmpty()) {
                        val membroEsistente = gruppoRepository.getMembro(currentGruppoId, currentAmicoId)
                        if (membroEsistente != null) {
                            val amicoAggiornato = membroEsistente.copy(
                                nome = _nome.value,
                                postiAuto = posti,
                                tipoCarburante = _tipoCarburante.value,
                                consumoMedio = consumo
                            )
                            gruppoRepository.aggiornaAnagraficaMembro(currentGruppoId, amicoAggiornato)
                            amicoRepository.aggiungiAmico(amicoAggiornato)
                        } else {
                            val amicoRubrica = amicoRepository.getAmicoPerId(currentAmicoId)
                            if (amicoRubrica != null) {
                                val amicoAggiornato = amicoRubrica.copy(
                                    nome = _nome.value,
                                    postiAuto = posti,
                                    tipoCarburante = _tipoCarburante.value,
                                    consumoMedio = consumo
                                )
                                amicoRepository.aggiungiAmico(amicoAggiornato)
                            }
                        }
                    } else {
                        val nuovoAmico = Amico(
                            id = UUID.randomUUID().toString(),
                            nome = _nome.value,
                            postiAuto = posti,
                            tipoCarburante = _tipoCarburante.value,
                            consumoMedio = consumo,
                            uscite = 0,
                            guide = 0,
                            km = 0
                        )
                        amicoRepository.aggiungiAmico(nuovoAmico)
                        gruppoRepository.aggiungiMembroAlGruppo(currentGruppoId, nuovoAmico)
                    }
                } else {
                    val amicoBase = if (currentAmicoId.isNotEmpty()) {
                        amicoRepository.getAmicoPerId(currentAmicoId)
                    } else null

                    val amicoDaSalvare = amicoBase?.copy(
                        nome = _nome.value,
                        postiAuto = posti,
                        tipoCarburante = _tipoCarburante.value,
                        consumoMedio = consumo
                    ) ?: Amico(
                        id = if (currentAmicoId.isNotEmpty()) currentAmicoId else UUID.randomUUID().toString(),
                        nome = _nome.value,
                        postiAuto = posti,
                        tipoCarburante = _tipoCarburante.value,
                        consumoMedio = consumo,
                        uscite = 0,
                        guide = 0,
                        km = 0
                    )
                    amicoRepository.aggiungiAmico(amicoDaSalvare)
                }
                onFinito()
            } catch (e: Exception) {
                e.printStackTrace()
                onFinito()
            }
        }
    }

    fun eliminaAmico(onEliminato: () -> Unit) {
        viewModelScope.launch {
            try {
                if (currentAmicoId.isNotEmpty()) {
                    amicoRepository.rimuoviAmico(currentAmicoId)
                    if (currentGruppoId.isNotEmpty()) {
                        gruppoRepository.rimuoviMembroDalGruppo(currentGruppoId, currentAmicoId)
                    }
                }
                onEliminato()
            } catch (e: Exception) {
                e.printStackTrace()
                onEliminato()
            }
        }
    }
}