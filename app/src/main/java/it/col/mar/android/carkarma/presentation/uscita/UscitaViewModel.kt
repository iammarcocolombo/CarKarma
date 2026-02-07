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

    // USIAMO LA CHIAVE DI GOOGLE MAPS
    private val MAPS_API_KEY = Config.GOOGLE_MAPS_KEY

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

    // --- CALCOLO CON GOOGLE DIRECTIONS API (Versione Ufficiale) ---
    fun calcolaDistanzaDaMaps() {
        val start = _indirizzoPartenza.value
        val end = _indirizzoDestinazione.value

        if (start.isBlank() || end.isBlank()) {
            _errorMessage.value = "Inserisci sia partenza che destinazione"
            return
        }

        // Controllo rapido se la chiave è vuota o sbagliata
        if (MAPS_API_KEY.isEmpty() || MAPS_API_KEY.contains("YOUR_")) {
            _errorMessage.value = "Configurazione Chiave Google Maps mancante!"
            return
        }

        viewModelScope.launch {
            _isLoadingMaps.value = true
            try {
                // Eseguiamo la chiamata in background
                val kmSolaAndata = withContext(Dispatchers.IO) {
                    fetchGoogleDistance(start, end)
                }

                // Se fetchGoogleDistance non lancia eccezioni, proseguiamo
                lastCalculatedOneWayKm = kmSolaAndata
                val multiplier = if (_isAndataRitorno.value) 2 else 1
                _kmTotali.value = kmSolaAndata * multiplier
                _errorMessage.value = null

            } catch (e: Exception) {
                // Mostriamo l'errore reale (es. "REQUEST_DENIED") per capire il problema
                _errorMessage.value = "${e.message}"
            } finally {
                _isLoadingMaps.value = false
            }
        }
    }

    // Funzione specifica per Google Maps (Parsing JSON)
    private fun fetchGoogleDistance(origin: String, destination: String): Int {
        try {
            val originEnc = URLEncoder.encode(origin, "UTF-8")
            val destEnc = URLEncoder.encode(destination, "UTF-8")

            // URL ufficiale Google Directions
            val url = "https://maps.googleapis.com/maps/api/directions/json?origin=$originEnc&destination=$destEnc&key=$MAPS_API_KEY"

            val jsonResponse = URL(url).readText()
            val jsonObject = JSONObject(jsonResponse)

            // Controllo dello Status di Google
            val status = jsonObject.getString("status")
            if (status != "OK") {
                Log.e("CarKarmaMaps", "Google API Error: $status")
                val msg = when(status) {
                    "REQUEST_DENIED" -> "Accesso negato. Controlla che la chiave sia attiva, il Billing abilitato e la 'Directions API' accesa."
                    "OVER_QUERY_LIMIT" -> "Quota superata o fatturazione non attiva su Google Cloud."
                    "ZERO_RESULTS" -> "Nessun percorso trovato tra questi indirizzi."
                    else -> "Errore Google Maps: $status"
                }
                throw Exception(msg)
            }

            // Parsing della risposta
            val routes = jsonObject.getJSONArray("routes")
            if (routes.length() > 0) {
                val legs = routes.getJSONObject(0).getJSONArray("legs")
                if (legs.length() > 0) {
                    val distance = legs.getJSONObject(0).getJSONObject("distance")
                    val meters = distance.getInt("value")
                    // Convertiamo metri in km (arrotondamento per eccesso)
                    return ((meters + 500) / 1000)
                }
            }
            throw Exception("Risposta vuota da Google Maps.")
        } catch (e: Exception) {
            // Rilanciamo l'eccezione per farla catturare sopra
            throw e
        }
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