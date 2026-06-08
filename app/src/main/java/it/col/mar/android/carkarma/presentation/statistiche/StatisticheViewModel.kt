package it.col.mar.android.carkarma.presentation.statistiche

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.col.mar.android.carkarma.data.model.Amico
import it.col.mar.android.carkarma.domain.CalcoloTurnoUseCase
import it.col.mar.android.carkarma.domain.repository.CarburanteRepository
import it.col.mar.android.carkarma.domain.repository.GruppoRepository
import it.col.mar.android.carkarma.domain.repository.UscitaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.abs

data class StatisticaMembro(
    val amico: Amico,
    val kmGuidati: Int,
    val usciteFatte: Int,
    val karma: Double,
    val percentuale: Float,
    val isSanto: Boolean
)

class StatisticheViewModel(
    private val gruppoRepository: GruppoRepository,
    private val uscitaRepository: UscitaRepository,
    private val carburanteRepository: CarburanteRepository
) : ViewModel() {

    private val _nomeGruppo = MutableStateFlow("")
    val nomeGruppo: StateFlow<String> = _nomeGruppo

    private val _kmTotaliGruppo = MutableStateFlow(0)
    val kmTotaliGruppo: StateFlow<Int> = _kmTotaliGruppo

    private val _numeroUscite = MutableStateFlow(0)
    val numeroUscite: StateFlow<Int> = _numeroUscite

    private val _mediaKm = MutableStateFlow(0)
    val mediaKm: StateFlow<Int> = _mediaKm

    private val _classifica = MutableStateFlow<List<StatisticaMembro>>(emptyList())
    val classifica: StateFlow<List<StatisticaMembro>> = _classifica

    private val _isRecalculating = MutableStateFlow(false)
    val isRecalculating: StateFlow<Boolean> = _isRecalculating

    private val calcoloUseCase = CalcoloTurnoUseCase()

    fun loadStatistiche(gruppoId: String) {
        viewModelScope.launch {
            val gruppo = gruppoRepository.getGruppoPerId(gruppoId)
            _nomeGruppo.value = gruppo?.nome ?: "Gruppo"

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
                    val bilanciMembri = membri.map { membro ->
                        val bilancioNetto = membro.bilanci.values.sum()
                        membro to bilancioNetto
                    }

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
                            isSanto = bilancioNetto >= -0.01
                        )
                    }

                    _classifica.value = stats.sortedByDescending { it.karma }
                } else {
                    _classifica.value = emptyList()
                }
            }
        }
    }

    fun ricalcolaTuttoLoStorico(gruppoId: String, onCompletato: () -> Unit) {
        viewModelScope.launch {
            _isRecalculating.value = true
            try {
                gruppoRepository.resetStatisticheMembri(gruppoId)

                val uscite = uscitaRepository.getUsciteSnapshot(gruppoId).sortedBy { it.data }
                val membri = gruppoRepository.getMembriSnapshot(gruppoId)
                val prezziCarburante = carburanteRepository.getPrezziAggiornati()

                for (u in uscite) {
                    val partecipantiIds = u.partecipantiIds.toSet()
                    val guidatoriIds = u.guidatoriIds.toSet()
                    val passeggeriIds = partecipantiIds - guidatoriIds

                    val numPartecipanti = partecipantiIds.size
                    if (numPartecipanti == 0 || guidatoriIds.isEmpty()) continue

                    val speseGuidatori = guidatoriIds.mapNotNull { driverId ->
                        val driver = membri.find { it.id == driverId } ?: return@mapNotNull null
                        val costoKm = calcoloUseCase.calcolaCostoChilometrico(driver, prezziCarburante)
                        val spesa = u.kmTotali * costoKm
                        driverId to spesa
                    }

                    val spesaTotale = speseGuidatori.sumOf { it.second }
                    val quotaPerPersona = spesaTotale / numPartecipanti

                    val creditiGuidatori = speseGuidatori.associate { (driverId, spesaDriver) ->
                        driverId to (spesaDriver - quotaPerPersona)
                    }

                    val totaleCreditiDaRecuperare = creditiGuidatori.values.sum()

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
                                            (deltaBilanci[passeggeroId] ?: 0.0) + quotaVersoQuestoDriver
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
                                        (deltaBilanci[driverId] ?: 0.0) - quotaVersoQuestoDriver
                                }
                            }
                        }

                        gruppoRepository.aggiornaStatisticheMembroP2P(
                            gruppoId = gruppoId,
                            amicoId = partecipanteId,
                            deltaUscite = 1,
                            deltaGuide = if (haGuidato) 1 else 0,
                            deltaKm = kmFisici,
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