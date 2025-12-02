package it.col.mar.android.carkarma.data.model

data class Uscita(
    val id: String = "",
    val gruppoId: String = "",
    val nome: String = "",
    val partecipantiIds: List<String> = emptyList(),
    val kmTotali: Int = 0,
    val guidatoriIds: List<String> = emptyList(),
    // Nuovi campi per salvare le posizioni
    val partenza: String = "",
    val destinazione: String = ""
)