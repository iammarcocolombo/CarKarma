package it.col.mar.android.carkarma.presentation.statistiche

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.col.mar.android.carkarma.domain.repository.GruppoRepository // CORRETTO: Importiamo l'interfaccia di dominio
import it.col.mar.android.carkarma.domain.repository.UscitaRepository // CORRETTO: Importiamo l'interfaccia di dominio
import it.col.mar.android.carkarma.data.model.Amico
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Dati per la riga della classifica
data class StatisticaMembro(
    val amico: Amico,
    val kmGuidati: Int,
    val usciteFatte: Int,     // Numero di uscite a cui ha partecipato
    val karma: Double,        // Km medi per uscita (Km / Uscite)
    val percentuale: Float,   // Per la barra grafica
    val isSanto: Boolean      // Se è sopra la media del gruppo
)

/**
 * ViewModel per la visualizzazione delle Statistiche del Gruppo.
 * Dipende esclusivamente dalle interfacce definite nel Domain Layer (Clean Architecture).
 */
class StatisticheViewModel(
    private val gruppoRepository: GruppoRepository, // CORRETTO: Tipo cambiato a Interfaccia
    private val uscitaRepository: UscitaRepository  // CORRETTO: Tipo cambiato a Interfaccia
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

    fun loadStatistiche(gruppoId: String) {
        viewModelScope.launch {
            // 1. Info Gruppo recuperate tramite interfaccia astratta
            val gruppo = gruppoRepository.getGruppoPerId(gruppoId)
            _nomeGruppo.value = gruppo?.nome ?: "Gruppo"

            // 2. Calcolo Totali dalle Uscite (Snapshot una-tantum)
            val uscite = uscitaRepository.getUsciteSnapshot(gruppoId)
            val kmTot = uscite.sumOf { it.kmTotali }
            val countUscite = uscite.size

            _kmTotaliGruppo.value = kmTot
            _numeroUscite.value = countUscite
            _mediaKm.value = if (countUscite > 0) kmTot / countUscite else 0

            // 3. Calcolo Classifica Karma Membri
            val membri = gruppoRepository.getMembriSnapshot(gruppoId)

            if (membri.isNotEmpty() && kmTot > 0) {
                val presenzeTotali = membri.sumOf { it.uscite }
                val mediaGruppoKarma = if (presenzeTotali > 0) kmTot.toDouble() / presenzeTotali else 0.0

                val stats = membri.map { membro ->
                    val guidati = membro.km
                    val presenze = membro.uscite.coerceAtLeast(1)

                    val karmaPunteggio = guidati.toDouble() / presenze
                    val perc = if (kmTot > 0) guidati.toFloat() / kmTot.toFloat() else 0f

                    StatisticaMembro(
                        amico = membro,
                        kmGuidati = guidati,
                        usciteFatte = membro.uscite,
                        karma = karmaPunteggio,
                        percentuale = perc,
                        isSanto = karmaPunteggio >= mediaGruppoKarma
                    )
                }

                _classifica.value = stats.sortedByDescending { it.karma }
            } else {
                _classifica.value = emptyList()
            }
        }
    }
}