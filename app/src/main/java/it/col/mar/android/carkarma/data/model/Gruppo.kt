package it.col.mar.android.carkarma.data.model

data class Gruppo(
    val id: String = "",
    val nome: String = "",
    // Salviamo solo gli ID per coerenza con Firebase.
    // Il ViewModel si occuperà di recuperare i dati degli amici usando questi ID.
    val membriIds: List<String> = emptyList()
)