package it.col.mar.android.carkarma.data.model

data class Uscita(
    val id: String = "",
    val nome: String = "",
    val gruppoId: String = "",           // Collegamento al gruppo (String)
    val partecipantiIds: List<String> = emptyList(), // Lista degli ID degli amici presenti
    val kmTotali: Int = 0,
    val guidatoriIds: List<String> = emptyList()     // Lista degli ID di chi ha guidato
)