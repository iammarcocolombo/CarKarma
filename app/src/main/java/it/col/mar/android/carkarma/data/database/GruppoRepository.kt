package it.col.mar.android.carkarma.data.database

import it.col.mar.android.carkarma.data.model.Gruppo
import it.col.mar.android.carkarma.data.model.Amico
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GruppoRepository(private val amicoRepository: AmicoRepository) {

    private val _gruppi = MutableStateFlow<List<Gruppo>>(emptyList())
    val gruppi: StateFlow<List<Gruppo>> = _gruppi.asStateFlow()

    init {
        val amici = amicoRepository.getTuttiGliAmici()

        val gruppo1 = Gruppo(1, "Gruppo Completo", amici)
        val gruppo2 = Gruppo(2, "Gruppo Marco e Luca", amici.filter { it.id == 1 || it.id == 2 })
        val gruppo3 = Gruppo(3, "Gruppo Luca e Giulia", amici.filter { it.id == 2 || it.id == 3 })

        _gruppi.value = listOf(gruppo1, gruppo2, gruppo3)
    }

    fun getTuttiIGruppi(): List<Gruppo> = _gruppi.value

    fun aggiungiGruppo(gruppo: Gruppo) {
        _gruppi.value = _gruppi.value + gruppo
    }

    fun eliminaGruppo(gruppoId: Int) {
        _gruppi.value = _gruppi.value.filterNot { it.id == gruppoId }
        AppContainer.uscitaRepository.eliminaUscitePerGruppo(gruppoId)
    }

    fun getGruppoPerId(id: Int): Gruppo? {
        return _gruppi.value.find { it.id == id }
    }

    fun generaNuovoId(): Int {
        return (_gruppi.value.maxOfOrNull { it.id } ?: 0) + 1
    }

    fun aggiornaGruppo(gruppo: Gruppo) {
        _gruppi.value = _gruppi.value.map {
            if (it.id == gruppo.id) gruppo else it
        }
    }
}
