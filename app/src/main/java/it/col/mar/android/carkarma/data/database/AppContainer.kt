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

    // Passiamo db e auth ai repository
    val amicoRepository = AmicoRepository(db, auth)
    val gruppoRepository = GruppoRepository(db, auth, amicoRepository)
    val uscitaRepository = UscitaRepository(db, auth)

    init {
        // Login Anonimo automatico all'avvio
        // Serve per avere un UID e poter scrivere sul database in sicurezza
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