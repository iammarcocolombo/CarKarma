package it.col.mar.android.carkarma.presentation.calcolo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.col.mar.android.carkarma.data.database.AmicoRepository
import it.col.mar.android.carkarma.data.database.GruppoRepository
import it.col.mar.android.carkarma.data.model.Amico
import it.col.mar.android.carkarma.data.model.Gruppo
import it.col.mar.android.carkarma.domain.CalcoloTurnoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CalcoloViewModel(
    private val gruppoRepository: GruppoRepository,
    private val amicoRepository: AmicoRepository
) : ViewModel() {

    private val useCase = CalcoloTurnoUseCase()

    // Stato UI
    val gruppi: StateFlow<List<Gruppo>> = gruppoRepository.gruppi

    private val _gruppoSelezionato = MutableStateFlow<Gruppo?>(null)
    val gruppoSelezionato: StateFlow<Gruppo?> = _gruppoSelezionato

    private val _membriDelGruppo = MutableStateFlow<List<Amico>>(emptyList())
    val membriDelGruppo: StateFlow<List<Amico>> = _membriDelGruppo

    // Chi è presente stasera?
    private val _presentiSelezionati = MutableStateFlow<Set<String>>(emptySet())
    val presentiSelezionati: StateFlow<Set<String>> = _presentiSelezionati

    // Risultato: Lista ordinata di chi deve guidare
    private val _risultatoCalcolo = MutableStateFlow<List<Pair<Amico, Double>>>(emptyList())
    val risultatoCalcolo: StateFlow<List<Pair<Amico, Double>>> = _risultatoCalcolo

    fun selezionaGruppo(gruppoId: String) {
        viewModelScope.launch {
            val gruppo = gruppoRepository.getGruppoPerId(gruppoId)
            _gruppoSelezionato.value = gruppo

            if (gruppo != null) {
                // Carichiamo i membri del gruppo
                val tuttiAmici = amicoRepository.getTuttiGliAmici()
                val membri = tuttiAmici.filter { gruppo.membriIds.contains(it.id) }
                _membriDelGruppo.value = membri

                // Di default, selezioniamo tutti come presenti
                _presentiSelezionati.value = membri.map { it.id }.toSet()

                // Resetta risultato precedente
                _risultatoCalcolo.value = emptyList()
            }
        }
    }

    fun togglePresente(amicoId: String) {
        val current = _presentiSelezionati.value.toMutableSet()
        if (current.contains(amicoId)) {
            current.remove(amicoId)
        } else {
            current.add(amicoId)
        }
        _presentiSelezionati.value = current
    }

    fun calcola() {
        val membri = _membriDelGruppo.value
        val presentiIds = _presentiSelezionati.value

        // Chiamiamo la logica pura
        val classifica = useCase.calcolaChiGuida(membri, presentiIds)
        _risultatoCalcolo.value = classifica
    }
}