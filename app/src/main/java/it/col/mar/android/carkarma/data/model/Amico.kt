package it.col.mar.android.carkarma.data.model

data class Amico(
    val id: String = java.util.UUID.randomUUID().toString(),
    val nome: String = "",
    val postiAuto: Int = 5,

    // Statistiche
    val uscite: Int = 0,
    val guide: Int = 0,
    val km: Int = 0, // Km fisici percorsi (per curiosità storica)

    // NUOVO: Punteggio Economico Accumulato ("Salvadanaio")
    // Questo valore cresce viaggio dopo viaggio in base all'auto che avevi IN QUEL MOMENTO.
    val karma: Double = 0.0,

    // Dati Auto Attuali (usati solo per i NUOVI viaggi)
    val tipoCarburante: String = "Benzina",
    val consumoMedio: Double = 0.0
)