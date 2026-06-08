package it.col.mar.android.carkarma.presentation.statistiche.dettaglio


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.col.mar.android.carkarma.data.model.Amico
import it.col.mar.android.carkarma.domain.repository.GruppoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class BilancioIncrociato(
    val nomeAltroMembro: String,
    val saldo: Double // Positivo = Credito, Negativo = Debito, 0.0 = Al pari
)

class DettaglioKarmaViewModel(
    private val gruppoRepository: GruppoRepository
) : ViewModel() {

    private val _nomeComponente = MutableStateFlow("")
    val nomeComponente: StateFlow<String> = _nomeComponente

    private val _listaBilanci = MutableStateFlow<List<BilancioIncrociato>>(emptyList())
    val listaBilanci: StateFlow<List<BilancioIncrociato>> = _listaBilanci

    fun loadDettagliComponente(gruppoId: String, componenteId: String) {
        viewModelScope.launch {
            val tuttiMembri = gruppoRepository.getMembriSnapshot(gruppoId)
            val componenteTarget = tuttiMembri.find { it.id == componenteId }

            if (componenteTarget != null) {
                _nomeComponente.value = componenteTarget.nome

                val bilanciMappati = tuttiMembri
                    .filter { it.id != componenteId } // Escludiamo se stesso
                    .map { altroMembro ->
                        val saldoSpecifico = componenteTarget.bilanci[altroMembro.id] ?: 0.0
                        BilancioIncrociato(
                            nomeAltroMembro = altroMembro.nome,
                            saldo = saldoSpecifico
                        )
                    }
                    .sortedByDescending { it.saldo }

                _listaBilanci.value = bilanciMappati
            }
        }
    }
}