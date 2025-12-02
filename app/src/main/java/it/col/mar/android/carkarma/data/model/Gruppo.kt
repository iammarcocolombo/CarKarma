package it.col.mar.android.carkarma.data.model

data class Gruppo(
    val id: String = "",
    val nome: String = "",

    // Questi sono gli ID degli amici "personaggi" del gruppo (quelli che guidano)
    val membriIds: List<String> = emptyList(),

    // NUOVO: Questi sono gli UID di Google delle persone reali che possono VEDERE e MODIFICARE questo gruppo
    // Se creo io il gruppo, il mio UID finisce qui. Se invito te, aggiungo il tuo UID qui.
    val utentiIds: List<String> = emptyList()
)