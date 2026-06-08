package it.col.mar.android.carkarma.data.model

data class Amico(
    val id: String = java.util.UUID.randomUUID().toString(),
    val nome: String = "",
    val postiAuto: Int = 5,

    val uscite: Int = 0,
    val guide: Int = 0,
    val km: Int = 0,

    // Chiave: ID dell'altro amico.
    // Valore: positivo = sono in credito con lui, negativo = sono in debito con lui
    val bilanci: Map<String, Double> = emptyMap(),

    val tipoCarburante: String = "Benzina",
    val consumoMedio: Double = 0.0
)