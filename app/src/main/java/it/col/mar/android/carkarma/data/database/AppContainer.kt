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

    // 1. Creiamo i repository che non dipendono da altri
    val amicoRepository = AmicoRepository(db, auth)
    val uscitaRepository = UscitaRepository(db, auth)

    // 2. Creiamo GruppoRepository passandogli le dipendenze necessarie
    val gruppoRepository = GruppoRepository(
        db,
        auth,
        amicoRepository,
        uscitaRepository
    )

    // NOTA: Abbiamo rimosso il blocco init con signInAnonymously().
    // Ora l'app richiede esplicitamente il login tramite GoogleAuthClient nel NavHost.
}