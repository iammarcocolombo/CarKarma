package it.col.mar.android.carkarma.presentation.statistiche

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.col.mar.android.carkarma.domain.CalcoloTurnoUseCase
import it.col.mar.android.carkarma.domain.repository.GruppoRepository
import it.col.mar.android.carkarma.domain.repository.UscitaRepository
import it.col.mar.android.carkarma.data.model.Amico
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.abs

// Dati per la riga della classifica
data class StatisticaMembro(
    val amico: Amico,
    val kmGuidati: Int,
    val usciteFatte: Int,
    val karma: Double,        // RAPPRESENTA IL BILANCIO NETTO IN EURO (+ Credito, - Debito)
    val percentuale: Float,   // Per la barra grafica (normalizzata sul debito/credito massimo)
    val isSanto: Boolean      // True se è in credito (>= 0), False se è in debito (< 0)
)

/**
 * ViewModel per la visualizzazione delle Statistiche del Gruppo.
 * Legge la rete dei debiti Peer-to-Peer (P2P) in modo sicuro e reattivo.
 */
class StatisticheViewModel(
    private val gruppoRepository: GruppoRepository,
    private val uscitaRepository: UscitaRepository
) : ViewModel() {

    private val _nomeGruppo = MutableStateFlow("")
    val nomeGruppo: StateFlow<String> = _nomeGruppo

    // Dati Header
    private val _kmTotaliGruppo = MutableStateFlow(0)
    val kmTotaliGruppo: StateFlow<Int> = _kmTotaliGruppo

    private val _numeroUscite = MutableStateFlow(0)
    val numeroUscite: StateFlow<Int> = _numeroUscite

    private val _mediaKm = MutableStateFlow(0)
    val mediaKm: StateFlow<Int> = _mediaKm

    // Dati Classifica
    private val _classifica = MutableStateFlow<List<StatisticaMembro>>(emptyList())
    val classifica: StateFlow<List<StatisticaMembro>> = _classifica

    // Stato ricalcolo di massa
    private val _isRecalculating = MutableStateFlow(false)
    val isRecalculating: StateFlow<Boolean> = _isRecalculating

    private val calcoloUseCase = CalcoloTurnoUseCase()

    fun loadStatistiche(gruppoId: String) {
        viewModelScope.launch {
            val gruppo = gruppoRepository.getGruppoPerId(gruppoId)
            _nomeGruppo.value = gruppo?.nome ?: "Gruppo"

            // SOLUZIONE ALLA RACE CONDITION: combine ascolta entrambi i flussi in parallelo sicuro
            combine(
                uscitaRepository.getUsciteDelGruppo(gruppoId),
                gruppoRepository.getMembriDelGruppo(gruppoId)
            ) { uscite, membri ->
                Pair(uscite, membri)
            }.collect { (uscite, membri) ->
                val kmTot = uscite.sumOf { it.kmTotali }
                val countUscite = uscite.size

                _kmTotaliGruppo.value = kmTot
                _numeroUscite.value = countUscite
                _mediaKm.value = if (countUscite > 0) kmTot / countUscite else 0

                if (membri.isNotEmpty()) {
                    // 1. Calcoliamo il Bilancio Netto P2P
                    val bilanciMembri = membri.map { membro ->
                        val bilancioNetto = membro.bilanci.values.sum()
                        membro to bilancioNetto
                    }

                    // 2. Troviamo il massimo per normalizzare le barre grafiche
                    val maxBilancioAssoluto = bilanciMembri.maxOfOrNull { abs(it.second) } ?: 1.0
                    val normalizzatore = if (maxBilancioAssoluto == 0.0) 1.0 else maxBilancioAssoluto

                    val stats = bilanciMembri.map { (membro, bilancioNetto) ->
                        val perc = (abs(bilancioNetto) / normalizzatore).toFloat()

                        StatisticaMembro(
                            amico = membro,
                            kmGuidati = membro.km,
                            usciteFatte = membro.uscite,
                            karma = bilancioNetto,
                            percentuale = perc,
                            isSanto = bilancioNetto >= -0.01 // Gestione approssimazione float
                        )
                    }

                    _classifica.value = stats.sortedByDescending { it.karma }
                } else {
                    _classifica.value = emptyList()
                }
            }
        }
    }

    /**
     * FUNZIONE DI MIGRAZIONE MASSIVA: Ricalcola sequenzialmente e in tempo reale
     * tutto lo storico delle uscite passate per la logica P2P (Mappa dei debiti).
     */
    fun ricalcolaTuttoLoStorico(gruppoId: String, onCompletato: () -> Unit) {
        viewModelScope.launch {
            _isRecalculating.value = true
            try {
                // 1. Azzeriamo le statistiche corrotte o vecchie sul Cloud in un'unica transazione Batch
                gruppoRepository.resetStatisticheMembri(gruppoId)

                // 2. Prendiamo le uscite ordinate storicamente e i membri base
                val uscite = uscitaRepository.getUsciteSnapshot(gruppoId).sortedBy { it.data }
                val membri = gruppoRepository.getMembriSnapshot(gruppoId)

                // Usiamo prezzi fissi qui dentro per non dipendere dalle API durante il ricalcolo massivo
                val prezziCarburante = mapOf(
                    "Benzina" to 1.800, "Diesel" to 1.700, "GPL" to 0.720,
                    "Metano" to 1.300, "Elettrico" to 0.500, "Ibrida" to 1.800
                )

                // 3. Eseguiamo il ricalcolo sequenziale per ogni uscita passata
                for (u in uscite) {
                    val numPartecipanti = u.partecipantiIds.size
                    if (numPartecipanti == 0) continue

                    val quoteGuidatori = u.guidatoriIds.mapNotNull { dId ->
                        val driver = membri.find { it.id == dId } ?: return@mapNotNull null
                        val costoKm = calcoloUseCase.calcolaCostoChilometrico(driver, prezziCarburante)
                        val spesaTotaleDriver = u.kmTotali * costoKm
                        val quotaSingolaA_Testa = spesaTotaleDriver / numPartecipanti
                        dId to quotaSingolaA_Testa
                    }

                    u.partecipantiIds.forEach { pId ->
                        val haGuidato = u.guidatoriIds.contains(pId)
                        val kmFisici = if (haGuidato) u.kmTotali else 0

                        var karmaLegacy = 0.0
                        val deltaBilanci = mutableMapOf<String, Double>()

                        for ((dId, quota) in quoteGuidatori) {
                            if (pId == dId) {
                                karmaLegacy += (quota * numPartecipanti)
                                u.partecipantiIds.forEach { altroId ->
                                    if (altroId != pId) {
                                        deltaBilanci[altroId] = (deltaBilanci[altroId] ?: 0.0) + quota
                                    }
                                }
                            } else {
                                deltaBilanci[dId] = (deltaBilanci[dId] ?: 0.0) - quota
                            }
                        }

                        // Scrittura sequenziale asincrona garantita: aspetta Firebase prima del loop successivo!
                        gruppoRepository.aggiornaStatisticheMembroP2P(
                            gruppoId = gruppoId,
                            amicoId = pId,
                            deltaUscite = 1,
                            deltaGuide = if (haGuidato) 1 else 0,
                            deltaKm = kmFisici,
                            deltaKarma = karmaLegacy,
                            deltaBilanci = deltaBilanci
                        )
                    }
                }
                onCompletato()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRecalculating.value = false
            }
        }
    }
}