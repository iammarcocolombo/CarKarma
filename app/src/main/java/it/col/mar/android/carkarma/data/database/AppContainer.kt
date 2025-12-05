package it.col.mar.android.carkarma.data.database

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage

object AppContainer {
    // Inizializziamo Firestore e Auth
    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = Firebase.auth

    // NUOVO: Inizializziamo lo Storage per le immagini (Foto profilo gruppi)
    private val storage: FirebaseStorage = Firebase.storage

    // 1. Creiamo i repository base
    val amicoRepository = AmicoRepository(db, auth)
    val uscitaRepository = UscitaRepository(db, auth)

    // 2. Creiamo GruppoRepository
    // GLI PASSIAMO 'storage' PER POTER CARICARE LE FOTO
    val gruppoRepository = GruppoRepository(
        db,
        auth,
        storage, // <--- Parametro aggiunto
        amicoRepository,
        uscitaRepository
    )
}