package it.col.mar.android.carkarma.data.model

data class Amico(
    val id: String = java.util.UUID.randomUUID().toString(),
    val nome: String = "",
    val postiAuto: Int = 5,

    val uscite: Int = 0,
    val guide: Int = 0,
    val km: Int = 0,

    // Manteniamo il Karma per visualizzare una statistica totale "semplice"
    val karma: Double = 0.0,

    // --- NUOVO CAMPO: MATRICE DEI BILANCI ---
    // Chiave: ID dell'altro amico.
    // Valore: Quanti Euro (Positivo = Sono in credito con lui, Negativo = Sono in debito con lui)
    val bilanci: Map<String, Double> = emptyMap(),

    val tipoCarburante: String = "Benzina",
    val consumoMedio: Double = 0.0
)