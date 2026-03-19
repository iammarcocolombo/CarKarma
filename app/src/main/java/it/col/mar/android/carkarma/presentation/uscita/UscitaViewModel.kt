package it.col.mar.android.carkarma.presentation.uscita

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.col.mar.android.carkarma.data.database.CarburanteRepository
import it.col.mar.android.carkarma.data.database.GruppoRepository
import it.col.mar.android.carkarma.data.database.UscitaRepository
import it.col.mar.android.carkarma.data.model.Amico
import it.col.mar.android.carkarma.data.model.Uscita
import it.col.mar.android.carkarma.domain.CalcoloTurnoUseCase
import it.col.mar.android.carkarma.util.Config
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
    private val gruppoRepository: GruppoRepository,
    private val carburanteRepository: CarburanteRepository
) : ViewModel() {

    private val MAPS_API_KEY = Config.GOOGLE_MAPS_KEY
    private val calcoloUseCase = CalcoloTurnoUseCase()

    private var currentUscitaId: String = ""
    private var currentGruppoId: String = ""
    private var originalUscita: Uscita? = null
    private var lastCalculatedOneWayKm: Int? = null

    private val _nomeUscita = MutableStateFlow("")
    val nomeUscita: StateFlow<String> = _nomeUscita

    private val _indirizzoPartenza = MutableStateFlow("")
    val indirizzoPartenza: StateFlow<String> = _indirizzoPartenza

    private val _indirizzoDestinazione = MutableStateFlow("")
    val indirizzoDestinazione: StateFlow<String> = _indirizzoDestinazione

    private val _isAndataRitorno = MutableStateFlow(true)
    val isAndataRitorno: StateFlow<Boolean> = _isAndataRitorno

    private val _isLoadingMaps = MutableStateFlow(false)
    val isLoadingMaps: StateFlow<Boolean> = _isLoadingMaps

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
                    originalUscita = uscita
                    _nomeUscita.value = uscita.nome
                    _partecipantiSelezionati.value = uscita.partecipantiIds.toSet()
                    _guidatoriSelezionati.value = uscita.guidatoriIds.toSet()
                    _kmTotali.value = uscita.kmTotali
                    _indirizzoPartenza.value = uscita.partenza
                    _indirizzoDestinazione.value = uscita.destinazione
                    _isAndataRitorno.value = uscita.andataRitorno
                }
            }
        }
    }

    fun onNomeUscitaChange(v: String) { _nomeUscita.value = v }
    fun onPartenzaChange(v: String) { _indirizzoPartenza.value = v }
    fun onDestinazioneChange(v: String) { _indirizzoDestinazione.value = v }

    fun onAndataRitornoChange(isAR: Boolean) {
        _isAndataRitorno.value = isAR
        if (lastCalculatedOneWayKm != null) {
            val multiplier = if (isAR) 2 else 1
            _kmTotali.value = lastCalculatedOneWayKm!! * multiplier
        }
    }

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

    fun setKmTotali(km: Int) {
        _kmTotali.value = km
        lastCalculatedOneWayKm = null
    }

    fun calcolaDistanzaDaMaps() {
        val start = _indirizzoPartenza.value
        val end = _indirizzoDestinazione.value

        if (start.isBlank() || end.isBlank()) {
            _errorMessage.value = "Inserisci sia partenza che destinazione"
            return
        }

        if (MAPS_API_KEY.isEmpty() || MAPS_API_KEY.contains("YOUR_")) {
            _errorMessage.value = "Configurazione Chiave Google Maps mancante!"
            return
        }

        viewModelScope.launch {
            _isLoadingMaps.value = true
            try {
                val kmSolaAndata = withContext(Dispatchers.IO) {
                    fetchGoogleDistance(start, end)
                }

                lastCalculatedOneWayKm = kmSolaAndata
                val multiplier = if (_isAndataRitorno.value) 2 else 1
                _kmTotali.value = kmSolaAndata * multiplier
                _errorMessage.value = null

            } catch (e: Exception) {
                _errorMessage.value = "Errore Maps: ${e.message}"
            } finally {
                _isLoadingMaps.value = false
            }
        }
    }

    private fun fetchGoogleDistance(origin: String, destination: String): Int {
        val originEnc = URLEncoder.encode(origin, "UTF-8")
        val destEnc = URLEncoder.encode(destination, "UTF-8")

        val url = "https://maps.googleapis.com/maps/api/directions/json?origin=$originEnc&destination=$destEnc&key=$MAPS_API_KEY"

        val jsonResponse = URL(url).readText()
        val jsonObject = JSONObject(jsonResponse)

        val status = jsonObject.getString("status")
        if (status != "OK") {
            Log.e("CarKarmaMaps", "Google API Error: $status")
            val msg = when(status) {
                "REQUEST_DENIED" -> "Accesso negato. Controlla Billing e API Key."
                "OVER_QUERY_LIMIT" -> "Quota superata."
                "ZERO_RESULTS" -> "Percorso non trovato."
                else -> "Errore Google: $status"
            }
            throw Exception(msg)
        }

        val routes = jsonObject.getJSONArray("routes")
        if (routes.length() > 0) {
            val legs = routes.getJSONObject(0).getJSONArray("legs")
            if (legs.length() > 0) {
                val distance = legs.getJSONObject(0).getJSONObject("distance")
                val meters = distance.getInt("value")
                return ((meters + 500) / 1000)
            }
        }
        throw Exception("Risposta vuota da Google Maps.")
    }

    fun calcolaSuggerimento() {
        val amici = _amiciDelGruppo.value
        val part = _partecipantiSelezionati.value
        if (part.size < 2) {
            _suggerimentoGuidatore.value = "Seleziona almeno 2 partecipanti."
            return
        }

        viewModelScope.launch {
            val res = calcoloUseCase.calcolaChiGuida(amici, part)

            if (res.isNotEmpty()) {
                val sb = StringBuilder("🚗 Consigliati:\n")
                // Il Bilancio ora rappresenta "Euro di credito/debito"
                res.forEach { (a, bilancioEuro) ->
                    val segno = if (bilancioEuro > 0) "+" else ""
                    sb.append("- ${a.nome} (Bilancio: $segno${String.format(Locale.US, "%.2f", bilancioEuro)}€)\n")
                }
                _suggerimentoGuidatore.value = sb.toString()
            } else {
                _suggerimentoGuidatore.value = "Nessuna soluzione trovata."
            }
        }
    }

    fun resetSuggerimento() { _suggerimentoGuidatore.value = null }
    fun clearError() { _errorMessage.value = null }

    fun salvaUscita(onSalvato: () -> Unit) {
        if (_partecipantiSelezionati.value.size < 2) {
            _errorMessage.value = "Servono almeno 2 partecipanti."
            return
        }

        viewModelScope.launch {
            val nuovaUscita = Uscita(
                id = if (currentUscitaId.isEmpty()) "" else currentUscitaId,
                nome = _nomeUscita.value,
                gruppoId = currentGruppoId,
                partecipantiIds = _partecipantiSelezionati.value.toList(),
                kmTotali = _kmTotali.value,
                guidatoriIds = _guidatoriSelezionati.value.toList(),
                partenza = _indirizzoPartenza.value,
                destinazione = _indirizzoDestinazione.value,
                andataRitorno = _isAndataRitorno.value
            )

            val prezzi = carburanteRepository.getPrezziAggiornati()

            if (currentUscitaId.isEmpty()) {
                uscitaRepository.aggiungiUscita(nuovaUscita)
                applicaStatistiche(nuovaUscita, prezzi)
            } else {
                originalUscita?.let { revertStatistiche(it, prezzi) }
                uscitaRepository.aggiornaUscita(nuovaUscita)
                applicaStatistiche(nuovaUscita, prezzi)
            }
            onSalvato()
        }
    }

    private suspend fun applicaStatistiche(u: Uscita, prezzi: Map<String, Double>) {
        u.partecipantiIds.forEach { id ->
            val amico = _amiciDelGruppo.value.find { it.id == id }
            val haGuidato = u.guidatoriIds.contains(id)
            val kmFisici = if (haGuidato) u.kmTotali else 0

            // Il Karma ora è letteralmente il costo in EURO del viaggio
            var euroSpesi = 0.0
            if (haGuidato && amico != null) {
                val costoKm = calcoloUseCase.calcolaCostoChilometrico(amico, prezzi)
                euroSpesi = kmFisici * costoKm
            }

            gruppoRepository.aggiornaStatisticheMembro(
                currentGruppoId, id,
                deltaUscite = 1,
                deltaGuide = if (haGuidato) 1 else 0,
                deltaKm = kmFisici,
                deltaKarma = euroSpesi // Aggiungiamo gli Euro spesi al salvadanaio
            )
        }
    }

    private suspend fun revertStatistiche(u: Uscita, prezzi: Map<String, Double>) {
        u.partecipantiIds.forEach { id ->
            val amico = _amiciDelGruppo.value.find { it.id == id }
            val haGuidato = u.guidatoriIds.contains(id)
            val kmFisici = if (haGuidato) u.kmTotali else 0

            var euroDaTogliere = 0.0
            if (haGuidato && amico != null) {
                val costoKm = calcoloUseCase.calcolaCostoChilometrico(amico, prezzi)
                euroDaTogliere = kmFisici * costoKm
            }

            gruppoRepository.aggiornaStatisticheMembro(
                currentGruppoId, id,
                deltaUscite = -1,
                deltaGuide = if (haGuidato) -1 else 0,
                deltaKm = -kmFisici,
                deltaKarma = -euroDaTogliere
            )
        }
    }

    fun eliminaUscita(onEliminato: () -> Unit) {
        viewModelScope.launch {
            if (currentUscitaId.isNotEmpty()) {
                val prezzi = carburanteRepository.getPrezziAggiornati()
                originalUscita?.let { revertStatistiche(it, prezzi) }
                uscitaRepository.eliminaUscita(currentGruppoId, currentUscitaId)
            }
            onEliminato()
        }
    }
}