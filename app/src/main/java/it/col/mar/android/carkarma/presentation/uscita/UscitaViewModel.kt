package it.col.mar.android.carkarma.presentation.uscita

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.col.mar.android.carkarma.data.database.GruppoRepository
import it.col.mar.android.carkarma.data.database.UscitaRepository
import it.col.mar.android.carkarma.data.model.Amico
import it.col.mar.android.carkarma.data.model.Uscita
import it.col.mar.android.carkarma.domain.CalcoloTurnoUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

class UscitaViewModel(
    private val uscitaRepository: UscitaRepository,
    private val gruppoRepository: GruppoRepository
) : ViewModel() {

    // TODO: Incolla qui la tua API Key di OpenRouteService (Gratis)
    // Prendila da: https://openrouteservice.org/dev/#/home
    private val ORS_API_KEY = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6ImQ0ZWE5MWY0MWQ5YjQ4NjQ4M2Q2NmUzYzU3YzBlOTE2IiwiaCI6Im11cm11cjY0In0="

    private val calcoloUseCase = CalcoloTurnoUseCase()

    private var currentUscitaId: String = ""
    private var currentGruppoId: String = ""

    private val _nomeUscita = MutableStateFlow("")
    val nomeUscita: StateFlow<String> = _nomeUscita

    // --- CAMPI INDIRIZZO ---
    private val _indirizzoPartenza = MutableStateFlow("")
    val indirizzoPartenza: StateFlow<String> = _indirizzoPartenza

    private val _indirizzoDestinazione = MutableStateFlow("")
    val indirizzoDestinazione: StateFlow<String> = _indirizzoDestinazione

    private val _isLoadingMaps = MutableStateFlow(false)
    val isLoadingMaps: StateFlow<Boolean> = _isLoadingMaps
    // -----------------------

    private val _amiciDelGruppo = MutableStateFlow<List<Amico>>(emptyList())
    val amiciDelGruppo: StateFlow<List<Amico>> = _amiciDelGruppo

    private val _partecipantiSelezionati = MutableStateFlow<Set<String>>(emptySet())
    val partecipantiSelezionati: StateFlow<Set<String>> = _partecipantiSelezionati

    private val _guidatoriSelezionati = MutableStateFlow<Set<String>>(emptySet())
    val guidatoriSelezionati: StateFlow<Set<String>> = _guidatoriSelezionati

    private val _kmTotali = MutableStateFlow(0)
    val kmTotali: StateFlow<Int> = _kmTotali

    private val _suggerimentoGuidatore = MutableStateFlow<String?>(null)
    val suggerimentoGuidatore: StateFlow<String?> = _suggerimentoGuidatore

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun loadUscita(gruppoId: String, uscitaId: String) {
        this.currentGruppoId = gruppoId
        this.currentUscitaId = uscitaId

        viewModelScope.launch {
            gruppoRepository.getMembriDelGruppo(gruppoId).collect { membri ->
                _amiciDelGruppo.value = membri
            }
        }

        if (uscitaId.isNotEmpty()) {
            viewModelScope.launch {
                val uscita = uscitaRepository.getUscita(gruppoId, uscitaId)
                if (uscita != null) {
                    _nomeUscita.value = uscita.nome
                    _partecipantiSelezionati.value = uscita.partecipantiIds.toSet()
                    _guidatoriSelezionati.value = uscita.guidatoriIds.toSet()
                    _kmTotali.value = uscita.kmTotali
                }
            }
        }
    }

    fun onNomeUscitaChange(nuovoNome: String) { _nomeUscita.value = nuovoNome }
    fun onPartenzaChange(addr: String) { _indirizzoPartenza.value = addr }
    fun onDestinazioneChange(addr: String) { _indirizzoDestinazione.value = addr }

    fun togglePartecipanteSelezionato(amicoId: String) {
        _partecipantiSelezionati.value = _partecipantiSelezionati.value.toMutableSet().apply {
            if (contains(amicoId)) {
                remove(amicoId)
                val nuoviGuidatori = _guidatoriSelezionati.value.toMutableSet()
                nuoviGuidatori.remove(amicoId)
                _guidatoriSelezionati.value = nuoviGuidatori
            } else {
                add(amicoId)
            }
        }
    }

    fun toggleGuidatoreSelezionato(amicoId: String) {
        if (_partecipantiSelezionati.value.contains(amicoId)) {
            _guidatoriSelezionati.value = _guidatoriSelezionati.value.toMutableSet().apply {
                if (contains(amicoId)) remove(amicoId) else add(amicoId)
            }
        }
    }

    fun setKmTotali(km: Int) { _kmTotali.value = km }

    // --- CALCOLO CON OPEN ROUTE SERVICE (ORS) ---
    fun calcolaDistanzaDaMaps() {
        val start = _indirizzoPartenza.value
        val end = _indirizzoDestinazione.value

        if (start.isBlank() || end.isBlank()) {
            _errorMessage.value = "Inserisci sia partenza che destinazione!"
            return
        }

        if (ORS_API_KEY.contains("LA_TUA_CHIAVE")) {
            _errorMessage.value = "Manca la API Key di ORS nel codice!"
            return
        }

        viewModelScope.launch {
            _isLoadingMaps.value = true
            try {
                // Eseguiamo tutto in un thread separato (IO)
                val km = withContext(Dispatchers.IO) {
                    // 1. Troviamo le coordinate di partenza
                    val startCoords = fetchCoordinates(start) ?: return@withContext null
                    // 2. Troviamo le coordinate di arrivo
                    val endCoords = fetchCoordinates(end) ?: return@withContext null
                    // 3. Calcoliamo il percorso
                    fetchRouteDistance(startCoords, endCoords)
                }

                if (km != null) {
                    _kmTotali.value = km
                    _errorMessage.value = null // Tutto ok
                } else {
                    _errorMessage.value = "Indirizzo non trovato o percorso non calcolabile."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Errore di connessione: ${e.localizedMessage}"
                Log.e("CarKarmaORS", "Errore", e)
            } finally {
                _isLoadingMaps.value = false
            }
        }
    }

    // Geocoding: Indirizzo -> Lat/Lon
    private fun fetchCoordinates(address: String): Pair<Double, Double>? {
        try {
            val encodedAddress = URLEncoder.encode(address, "UTF-8")
            val urlString = "https://api.openrouteservice.org/geocode/search?api_key=$ORS_API_KEY&text=$encodedAddress&size=1"

            val jsonResponse = URL(urlString).readText()
            val jsonObject = JSONObject(jsonResponse)

            val features = jsonObject.getJSONArray("features")
            if (features.length() > 0) {
                val geometry = features.getJSONObject(0).getJSONObject("geometry")
                val coordinates = geometry.getJSONArray("coordinates")
                // ORS restituisce [Longitudine, Latitudine]
                val lon = coordinates.getDouble(0)
                val lat = coordinates.getDouble(1)
                return Pair(lon, lat)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    // Routing: Lat/Lon -> Distanza Km
    private fun fetchRouteDistance(start: Pair<Double, Double>, end: Pair<Double, Double>): Int? {
        try {
            val startStr = "${start.first},${start.second}"
            val endStr = "${end.first},${end.second}"

            val urlString = "https://api.openrouteservice.org/v2/directions/driving-car?api_key=$ORS_API_KEY&start=$startStr&end=$endStr"

            val jsonResponse = URL(urlString).readText()
            val jsonObject = JSONObject(jsonResponse)

            val features = jsonObject.getJSONArray("features")
            if (features.length() > 0) {
                val props = features.getJSONObject(0).getJSONObject("properties")
                val summary = props.getJSONObject("summary")
                val distanceMeters = summary.getDouble("distance")
                // Convertiamo metri in km
                return (distanceMeters / 1000).toInt()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    // --- ALGORITMO SUGGERIMENTO ---
    fun calcolaSuggerimento() {
        val amiciOggetti = _amiciDelGruppo.value
        val partecipantiIds = _partecipantiSelezionati.value

        if (partecipantiIds.size < 2) {
            _suggerimentoGuidatore.value = "Seleziona almeno 2 partecipanti."
            return
        }

        val classifica = calcoloUseCase.calcolaChiGuida(amiciOggetti, partecipantiIds)

        if (classifica.isNotEmpty()) {
            if (classifica.size == 1) {
                val (prescelto, karma) = classifica.first()
                _suggerimentoGuidatore.value = "🚗 Dovrebbe guidare:\n${prescelto.nome}\n(Karma Gruppo: ${String.format(Locale.US, "%.1f", karma)})"
            } else {
                val sb = StringBuilder("🚗 Servono ${classifica.size} auto!\nEcco la squadra ideale:\n\n")
                classifica.forEachIndexed { index, (amico, karma) ->
                    sb.append("${index + 1}. ${amico.nome} (${amico.postiAuto} posti)\n   Karma Gruppo: ${String.format(Locale.US, "%.1f", karma)}\n")
                }

                val postiTotali = classifica.sumOf { it.first.postiAuto }
                if (postiTotali < partecipantiIds.size) {
                    sb.append("\n⚠️ ATTENZIONE: Mancano ancora ${partecipantiIds.size - postiTotali} posti!")
                }
                _suggerimentoGuidatore.value = sb.toString()
            }
        } else {
            _suggerimentoGuidatore.value = "Nessuna soluzione trovata! Controlla i posti auto disponibili."
        }
    }

    fun resetSuggerimento() { _suggerimentoGuidatore.value = null }
    fun clearError() { _errorMessage.value = null }

    // --- SALVATAGGIO ---
    fun salvaUscita(onSalvato: () -> Unit) {
        if (_partecipantiSelezionati.value.size < 2) {
            _errorMessage.value = "Un'uscita richiede almeno 2 partecipanti."
            return
        }

        viewModelScope.launch {
            val partecipantiList = _partecipantiSelezionati.value.toList()
            val guidatoriList = _guidatoriSelezionati.value.toList()

            val uscita = Uscita(
                id = if (currentUscitaId.isEmpty()) "" else currentUscitaId,
                nome = _nomeUscita.value,
                gruppoId = currentGruppoId,
                partecipantiIds = partecipantiList,
                kmTotali = _kmTotali.value,
                guidatoriIds = guidatoriList
            )

            if (currentUscitaId.isEmpty()) {
                uscitaRepository.aggiungiUscita(uscita)
                aggiornaStatisticheMembriGruppo(partecipantiList, guidatoriList, _kmTotali.value)
            } else {
                uscitaRepository.aggiornaUscita(uscita)
            }
            onSalvato()
        }
    }

    private fun aggiornaStatisticheMembriGruppo(partecipanti: List<String>, guidatori: List<String>, km: Int) {
        partecipanti.forEach { id ->
            val haGuidato = guidatori.contains(id)
            val kmGuidatiReali = if (haGuidato) km else 0
            gruppoRepository.aggiornaStatisticheMembro(currentGruppoId, id, kmGuidatiReali, haGuidato)
        }
    }

    fun eliminaUscita(onEliminato: () -> Unit) {
        viewModelScope.launch {
            if (currentUscitaId.isNotEmpty()) {
                uscitaRepository.eliminaUscita(currentGruppoId, currentUscitaId)
            }
            onEliminato()
        }
    }
}