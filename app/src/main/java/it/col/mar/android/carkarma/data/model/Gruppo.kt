package it.col.mar.android.carkarma.data.model

data class Gruppo(
    val id: Int,
    val nome: String,
    val amici: List<Amico>
)
