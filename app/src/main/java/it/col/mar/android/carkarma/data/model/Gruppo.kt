package it.col.mar.android.carkarma.data.model

data class Gruppo(
    val id: String = "",
    val nome: String = "",
    val membriIds: List<String> = emptyList(),
    val utentiIds: List<String> = emptyList(),
    val avatarIndex: Int = 0
)