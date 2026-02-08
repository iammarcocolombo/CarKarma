package it.col.mar.android.carkarma.data.database

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.URL

class CarburanteRepository(
    private val db: FirebaseFirestore
) {

    private val prezziDefault = mapOf(
        "Benzina" to 1.800,
        "Diesel" to 1.700,
        "GPL" to 0.720,
        "Metano" to 1.300,
        "Elettrico" to 0.500,
        "Ibrida" to 1.800
    )

    private val MINISTERO_CSV_URL = "https://dgsaie.mise.gov.it/open_data_export.php?export-id=2&export-type=csv"

    suspend fun getPrezziAggiornati(): Map<String, Double> {
        val prezziMappa = prezziDefault.toMutableMap()

        // 1. Ministero
        try {
            val prezziMinistero = fetchPrezziMinistero()
            if (prezziMinistero.isNotEmpty()) {
                prezziMappa.putAll(prezziMinistero)
                return prezziMappa
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Firestore (Backup)
        try {
            val snapshot = db.collection("configurazione").document("prezzi_carburante").get().await()
            if (snapshot.exists() && snapshot.data != null) {
                snapshot.data!!.forEach { (key, value) ->
                    val prezzo = when (value) {
                        is Number -> value.toDouble()
                        is String -> value.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                    if (prezzo > 0) prezziMappa[key] = prezzo
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        return prezziMappa
    }

    private suspend fun fetchPrezziMinistero(): Map<String, Double> {
        return withContext(Dispatchers.IO) {
            val mappa = mutableMapOf<String, Double>()
            try {
                val csvContent = URL(MINISTERO_CSV_URL).readText()
                val righe = csvContent.lines()

                for (i in 1 until righe.size) {
                    val riga = righe[i]
                    val colonne = riga.split(",")

                    if (colonne.size >= 3) {
                        val nomeProdotto = colonne[1].replace("\"", "").trim()
                        val prezzoGrezzo = colonne[2].replace("\"", "").trim().toDoubleOrNull()

                        if (prezzoGrezzo != null) {
                            val prezzoEuro = prezzoGrezzo / 1000.0
                            when (nomeProdotto) {
                                "Benzina", "Super senza piombo" -> {
                                    mappa["Benzina"] = prezzoEuro
                                    mappa["Ibrida"] = prezzoEuro
                                }
                                "Gasolio", "Gasolio auto" -> mappa["Diesel"] = prezzoEuro
                                "GPL", "Gpl" -> mappa["GPL"] = prezzoEuro
                                "Metano" -> mappa["Metano"] = prezzoEuro
                            }
                        }
                    }
                }
            } catch (e: Exception) { throw e }
            mappa
        }
    }
}