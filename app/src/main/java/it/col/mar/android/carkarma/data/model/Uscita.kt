package it.col.mar.android.carkarma.data.model

data class Uscita(
    val id: String = "",
    val gruppoId: String = "",
    val nome: String = "",
    val partecipantiIds: List<String> = emptyList(),
    val kmTotali: Int = 0,
    val guidatoriIds: List<String> = emptyList(),
    val partenza: String = "",
    val destinazione: String = "",
    val andataRitorno: Boolean = true,
    // AGGIUNTO: Timestamp per ordinare (default: adesso)
    val data: Long = System.currentTimeMillis()
)