package it.col.mar.android.carkarma.data.model

data class Uscita(
    val id: Int,
    val nome: String,
    val partecipanti: List<Amico>,
    val kmTotali: Int,
    val guidatori: List<Amico>
)
