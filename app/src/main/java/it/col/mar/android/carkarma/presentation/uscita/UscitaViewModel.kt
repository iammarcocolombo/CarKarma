package it.col.mar.android.carkarma.presentation.uscita

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val gruppoRepository: GruppoRepository
) : ViewModel() {

    // Torniamo a usare la chiave di OpenRouteService dal file Config
    private val ORS_API_KEY = Config.ORS_API_KEY

    private val calcoloUseCase = CalcoloTurnoUseCase()

    private var currentUscitaId: String = ""
    private var currentGruppoId: String = ""

    // Salviamo l'uscita originale per poter fare il "revert" delle statistiche in caso di modifica
    private var originalUscita: Uscita? = null

    // Memorizza i km di sola andata calcolati dall'API per ricalcoli veloci dello switch A/R
    private var lastCalculatedOneWayKm: Int? = null

    private val _nomeUscita = MutableStateFlow("")
    val nomeUscita: StateFlow<String> = _nomeUscita

    // --- CAMPI INDIRIZZO ---
    private val _indirizzoPartenza = MutableStateFlow("")
    val indirizzoPartenza: StateFlow<String> = _indirizzoPartenza

    private val _indirizzoDestinazione = MutableStateFlow("")
    val indirizzoDestinazione: StateFlow<String> = _indirizzoDestinazione

    // Stato Andata/Ritorno
    private val _isAndataRitorno = MutableStateFlow(true)
    val isAndataRitorno: StateFlow<Boolean> = _isAndataRitorno

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

        // 1. Carichiamo i membri dal GRUPPO (sottocollezione)
        viewModelScope.launch {
            gruppoRepository.getMembriDelGruppo(gruppoId).collect { membri ->
                _amiciDelGruppo.value = membri
            }
        }

        // 2. Se è una modifica, carichiamo i dati dell'uscita
        if (uscitaId.isNotEmpty()) {
            viewModelScope.launch {
                val uscita = uscitaRepository.getUscita(gruppoId, uscitaId)
                if (uscita != null) {
                    originalUscita = uscita // Importante: salviamo copia per il rollback

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

    // Gestione cambio Andata/Ritorno con ricalcolo immediato
    fun onAndataRitornoChange(isAR: Boolean) {
        _isAndataRitorno.value = isAR
        // Se abbiamo un valore base calcolato dall'API, lo usiamo per aggiornare i km
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
        // Se l'utente modifica a mano, rompiamo il legame con il calcolo automatico precedente
        lastCalculatedOneWayKm = null
    }

    // --- CALCOLO CON OPEN ROUTE SERVICE (ORS) ---
    fun calcolaDistanzaDaMaps() {
        val start = _indirizzoPartenza.value
        val end = _indirizzoDestinazione.value

        if (start.isBlank() || end.isBlank()) {
            _errorMessage.value = "Inserisci sia partenza che destinazione"
            return
        }

        if (ORS_API_KEY.contains("YOUR_ORS_API_KEY") || ORS_API_KEY.isEmpty()) {
            _errorMessage.value = "Manca la API Key di ORS in Config.kt!"
            return
        }

        viewModelScope.launch {
            _isLoadingMaps.value = true
            try {
                val kmSolaAndata = withContext(Dispatchers.IO) {
                    // 1. Troviamo le coordinate di partenza
                    val sCoords = fetchCoordinates(start) ?: throw Exception("Indirizzo partenza non trovato")
                    // 2. Troviamo le coordinate di destinazione
                    val eCoords = fetchCoordinates(end) ?: throw Exception("Indirizzo destinazione non trovato")
                    // 3. Calcoliamo il percorso
                    fetchRouteDistance(sCoords, eCoords)
                }

                if (kmSolaAndata != null) {
                    lastCalculatedOneWayKm = kmSolaAndata
                    val multiplier = if (_isAndataRitorno.value) 2 else 1
                    _kmTotali.value = kmSolaAndata * multiplier
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "Percorso non trovato."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Errore: ${e.message}"
            } finally {
                _isLoadingMaps.value = false
            }
        }
    }

    // Geocoding con ORS
    private fun fetchCoordinates(address: String): Pair<Double, Double>? {
        try {
            val enc = URLEncoder.encode(address, "UTF-8")
            val url = "https://api.openrouteservice.org/geocode/search?api_key=$ORS_API_KEY&text=$enc&size=1"
            val json = JSONObject(URL(url).readText())
            val feats = json.getJSONArray("features")
            if (feats.length() > 0) {
                val coords = feats.getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                return Pair(coords.getDouble(0), coords.getDouble(1))
            }
        } catch (e: Exception) { e.printStackTrace() }
        return null
    }

    // Routing con ORS
    private fun fetchRouteDistance(start: Pair<Double, Double>, end: Pair<Double, Double>): Int? {
        try {
            val s = "${start.first},${start.second}"
            val e = "${end.first},${end.second}"
            val url = "https://api.openrouteservice.org/v2/directions/driving-car?api_key=$ORS_API_KEY&start=$s&end=$e"
            val json = JSONObject(URL(url).readText())
            val feats = json.getJSONArray("features")
            if (feats.length() > 0) {
                val dist = feats.getJSONObject(0).getJSONObject("properties").getJSONObject("summary").getDouble("distance")
                // Convertiamo metri in km con arrotondamento
                return ((dist / 1000) + 0.5).toInt()
            }
        } catch (e: Exception) { e.printStackTrace() }
        return null
    }

    // --- ALGORITMO SUGGERIMENTO ---
    fun calcolaSuggerimento() {
        val amici = _amiciDelGruppo.value
        val part = _partecipantiSelezionati.value
        if (part.size < 2) {
            _suggerimentoGuidatore.value = "Seleziona almeno 2 partecipanti."
            return
        }
        val res = calcoloUseCase.calcolaChiGuida(amici, part)
        if (res.isNotEmpty()) {
            val sb = StringBuilder("🚗 Consigliati:\n")
            res.forEach { (a, k) -> sb.append("- ${a.nome} (Karma: ${String.format(Locale.US, "%.1f", k)})\n") }
            _suggerimentoGuidatore.value = sb.toString()
        } else {
            _suggerimentoGuidatore.value = "Nessuna soluzione trovata. Controlla i posti auto."
        }
    }

    fun resetSuggerimento() { _suggerimentoGuidatore.value = null }
    fun clearError() { _errorMessage.value = null }

    // --- SALVATAGGIO & ROLLBACK ---
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

            if (currentUscitaId.isEmpty()) {
                // CREAZIONE
                uscitaRepository.aggiungiUscita(nuovaUscita)
                // Applica le nuove statistiche
                applicaStatistiche(nuovaUscita)
            } else {
                // MODIFICA
                // 1. Annulla l'effetto dell'uscita precedente (sottrai km e presenze)
                originalUscita?.let { revertStatistiche(it) }

                // 2. Salva la nuova versione
                uscitaRepository.aggiornaUscita(nuovaUscita)

                // 3. Applica le nuove statistiche
                applicaStatistiche(nuovaUscita)
            }
            onSalvato()
        }
    }

    // Aggiunge km e presenze (Delta POSITIVI)
    private fun applicaStatistiche(u: Uscita) {
        u.partecipantiIds.forEach { id ->
            val haGuidato = u.guidatoriIds.contains(id)
            val km = if (haGuidato) u.kmTotali else 0
            // +1 uscita, +1 guida, +km
            gruppoRepository.aggiornaStatisticheMembro(
                currentGruppoId, id,
                deltaUscite = 1,
                deltaGuide = if (haGuidato) 1 else 0,
                deltaKm = km
            )
        }
    }

    // Rimuove km e presenze (Delta NEGATIVI)
    private fun revertStatistiche(u: Uscita) {
        u.partecipantiIds.forEach { id ->
            val haGuidato = u.guidatoriIds.contains(id)
            val km = if (haGuidato) u.kmTotali else 0
            // -1 uscita, -1 guida, -km
            gruppoRepository.aggiornaStatisticheMembro(
                currentGruppoId, id,
                deltaUscite = -1,
                deltaGuide = if (haGuidato) -1 else 0,
                deltaKm = -km
            )
        }
    }

    fun eliminaUscita(onEliminato: () -> Unit) {
        viewModelScope.launch {
            if (currentUscitaId.isNotEmpty()) {
                // Se eliminiamo l'uscita, dobbiamo togliere le sue statistiche dal conteggio!
                originalUscita?.let { revertStatistiche(it) }
                uscitaRepository.eliminaUscita(currentGruppoId, currentUscitaId)
            }
            onEliminato()
        }
    }
}