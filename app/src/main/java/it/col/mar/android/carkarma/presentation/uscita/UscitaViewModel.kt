package it.col.mar.android.carkarma.presentation.uscita

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.col.mar.android.carkarma.data.model.Amico
import it.col.mar.android.carkarma.data.model.Uscita
import it.col.mar.android.carkarma.domain.CalcoloTurnoUseCase
import it.col.mar.android.carkarma.domain.repository.CarburanteRepository
import it.col.mar.android.carkarma.domain.repository.GruppoRepository
import it.col.mar.android.carkarma.domain.repository.UscitaRepository
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

    private val mapsAPIKEY = Config.GOOGLE_MAPS_KEY
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
        currentGruppoId = gruppoId
        currentUscitaId = uscitaId

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
        lastCalculatedOneWayKm?.let { kmBase ->
            _kmTotali.value = kmBase * if (isAR) 2 else 1
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

        if (mapsAPIKEY.isEmpty() || mapsAPIKEY.contains("YOUR_")) {
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
                _kmTotali.value = kmSolaAndata * if (_isAndataRitorno.value) 2 else 1
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
        val url = "https://maps.googleapis.com/maps/api/directions/json?origin=$originEnc&destination=$destEnc&key=$mapsAPIKEY"

        val jsonResponse = URL(url).readText()
        val jsonObject = JSONObject(jsonResponse)
        val status = jsonObject.getString("status")

        if (status != "OK") {
            val msg = when (status) {
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
                return (meters + 500) / 1000
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
                elaboraStatisticheP2P(nuovaUscita, prezzi, isRevert = false)
            } else {
                originalUscita?.let { elaboraStatisticheP2P(it, prezzi, isRevert = true) }
                uscitaRepository.aggiornaUscita(nuovaUscita)
                elaboraStatisticheP2P(nuovaUscita, prezzi, isRevert = false)
            }

            onSalvato()
        }
    }

    private suspend fun elaboraStatisticheP2P(
        u: Uscita,
        prezzi: Map<String, Double>,
        isRevert: Boolean
    ) {
        val partecipantiIds = u.partecipantiIds.toSet()
        val guidatoriIds = u.guidatoriIds.toSet()
        val passeggeriIds = partecipantiIds - guidatoriIds

        val numPartecipanti = partecipantiIds.size
        if (numPartecipanti == 0 || guidatoriIds.isEmpty()) return

        val multiplier = if (isRevert) -1.0 else 1.0
        val statMultiplier = if (isRevert) -1 else 1

        val speseGuidatori = guidatoriIds.mapNotNull { driverId ->
            val driver = _amiciDelGruppo.value.find { it.id == driverId } ?: return@mapNotNull null
            val costoKm = calcoloUseCase.calcolaCostoChilometrico(driver, prezzi)
            val spesa = u.kmTotali * costoKm
            driverId to spesa
        }

        val spesaTotale = speseGuidatori.sumOf { it.second }
        val quotaPerPersona = spesaTotale / numPartecipanti

        val creditiGuidatori = speseGuidatori.associate { (driverId, spesaDriver) ->
            driverId to (spesaDriver - quotaPerPersona)
        }

        val totaleCreditiDaRecuperare = creditiGuidatori.values.sum()

        Log.d("CarKarmaP2P", "--- INIZIO CALCOLO P2P EQUO --- Uscita: ${u.nome} (Revert: $isRevert)")
        Log.d("CarKarmaP2P", "Spesa totale: $spesaTotale, quota per persona: $quotaPerPersona")
        Log.d("CarKarmaP2P", "Crediti guidatori: $creditiGuidatori")

        partecipantiIds.forEach { partecipanteId ->
            val haGuidato = guidatoriIds.contains(partecipanteId)
            val kmFisici = if (haGuidato) u.kmTotali else 0
            val deltaBilanci = mutableMapOf<String, Double>()

            if (haGuidato) {
                if (passeggeriIds.isNotEmpty()) {
                    val creditoDriver = creditiGuidatori[partecipanteId] ?: 0.0

                    passeggeriIds.forEach { passeggeroId ->
                        val quotaVersoQuestoDriver =
                            if (totaleCreditiDaRecuperare > 0.0) {
                                quotaPerPersona * (creditoDriver / totaleCreditiDaRecuperare)
                            } else {
                                0.0
                            }

                        if (quotaVersoQuestoDriver != 0.0) {
                            deltaBilanci[passeggeroId] =
                                (deltaBilanci[passeggeroId] ?: 0.0) + (quotaVersoQuestoDriver * multiplier)
                        }
                    }
                }
            } else {
                creditiGuidatori.forEach { (driverId, creditoDriver) ->
                    val quotaVersoQuestoDriver =
                        if (totaleCreditiDaRecuperare > 0.0) {
                            quotaPerPersona * (creditoDriver / totaleCreditiDaRecuperare)
                        } else {
                            0.0
                        }

                    if (quotaVersoQuestoDriver != 0.0) {
                        deltaBilanci[driverId] =
                            (deltaBilanci[driverId] ?: 0.0) - (quotaVersoQuestoDriver * multiplier)
                    }
                }
            }

            Log.d("CarKarmaP2P", "Partecipante $partecipanteId -> $deltaBilanci")

            gruppoRepository.aggiornaStatisticheMembroP2P(
                gruppoId = currentGruppoId,
                amicoId = partecipanteId,
                deltaUscite = 1 * statMultiplier,
                deltaGuide = (if (haGuidato) 1 else 0) * statMultiplier,
                deltaKm = kmFisici * statMultiplier,
                deltaBilanci = deltaBilanci
            )
        }

        Log.d("CarKarmaP2P", "--- FINE CALCOLO P2P EQUO ---")
    }

    fun eliminaUscita(onEliminato: () -> Unit) {
        viewModelScope.launch {
            if (currentUscitaId.isNotEmpty()) {
                val prezzi = carburanteRepository.getPrezziAggiornati()
                originalUscita?.let { elaboraStatisticheP2P(it, prezzi, isRevert = true) }
                uscitaRepository.eliminaUscita(currentGruppoId, currentUscitaId)
            }
            onEliminato()
        }
    }
}