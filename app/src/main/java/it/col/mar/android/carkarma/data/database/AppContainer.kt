package it.col.mar.android.carkarma.data.database

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage

object AppContainer {
    // Inizializziamo i servizi Firebase
    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = Firebase.auth
    private val storage: FirebaseStorage = Firebase.storage

    // 1. Creiamo i repository base
    val amicoRepository = AmicoRepository(db, auth)
    val uscitaRepository = UscitaRepository(db, auth)

    // 2. Repository per i prezzi carburante (Nuovo)
    val carburanteRepository = CarburanteRepository(db)

    // 3. Creiamo GruppoRepository
    // Questo dipende dagli altri e dallo storage per le immagini
    val gruppoRepository = GruppoRepository(
        db,
        auth,
        storage,
        amicoRepository,
        uscitaRepository
    )
}