package it.col.mar.android.carkarma.data.model

data class Gruppo(
    val id: String = "",
    val nome: String = "",
    // Lista degli ID dei membri "personaggi" (amici) nel gruppo
    val membriIds: List<String> = emptyList(),
    // Lista degli UID degli utenti reali che hanno accesso al gruppo
    val utentiIds: List<String> = emptyList(),
    // NUOVO CAMPO: Indice dell'icona scelta (da 0 a 14 nella lista AvatarProvider)
    // Default = 0 (Icona Gruppi standard)
    val avatarIndex: Int = 0
)