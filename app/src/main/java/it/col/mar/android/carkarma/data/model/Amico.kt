package it.col.mar.android.carkarma.data.model

data class Amico(
    val id: String = java.util.UUID.randomUUID().toString(),
    val nome: String = "",
    val postiAuto: Int = 5,
    // Statistiche (per l'algoritmo)
    val uscite: Int = 0,
    val guide: Int = 0,
    val km: Int = 0,
    // NUOVI CAMPI AUTO
    val tipoCarburante: String = "Benzina", // Default
    val consumoMedio: Double = 0.0 // Litri per 100km (o kWh per 100km)
)