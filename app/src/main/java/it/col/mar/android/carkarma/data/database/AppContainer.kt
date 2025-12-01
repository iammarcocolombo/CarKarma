package it.col.mar.android.carkarma.data.database

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object AppContainer {
    // Inizializziamo Firestore e Auth
    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = Firebase.auth

    // 1. Creiamo i repository che non dipendono da altri (o dipendono solo da DB/Auth)
    val amicoRepository = AmicoRepository(db, auth)
    val uscitaRepository = UscitaRepository(db, auth)

    // 2. Creiamo GruppoRepository passandogli le dipendenze necessarie
    // Ora gli passiamo anche uscitaRepository per gestire l'eliminazione a cascata
    val gruppoRepository = GruppoRepository(
        db,
        auth,
        amicoRepository,
        uscitaRepository
    )

    init {
        // Login Anonimo automatico all'avvio
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnSuccessListener {
                    println("Login anonimo riuscito: ${it.user?.uid}")
                }
                .addOnFailureListener {
                    println("Errore login anonimo: $it")
                }
        }
    }
}