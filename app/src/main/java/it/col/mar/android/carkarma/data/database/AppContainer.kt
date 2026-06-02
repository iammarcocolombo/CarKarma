package it.col.mar.android.carkarma.data.database

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import it.col.mar.android.carkarma.domain.repository.*

/**
 * Dependency Injection manuale centralizzata per tutta l'applicazione.
 * Risolve i conflitti esponendo le interfacce del dominio ed inserendo alias per retrocompatibilità.
 */
object AppContainer {
    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = Firebase.auth
    private val storage: FirebaseStorage = Firebase.storage

    // 1. Repository del dominio concretizzati tramite le classi Impl
    val amicoRepository: AmicoRepository = AmicoRepositoryImpl(db, auth)
    val uscitaRepository: UscitaRepository = UscitaRepositoryImpl(db)
    val carburanteRepository: CarburanteRepository = CarburanteRepositoryImpl(db)

    val gruppoRepository: GruppoRepository = GruppoRepositoryImpl(
        db = db,
        auth = auth,
        storage = storage,
        amicoRepository = amicoRepository,
        uscitaRepository = uscitaRepository
    )

    // --- ALIAS DI COMPATIBILITÀ PER RETROCOMPATIBILITÀ CON LA HOME E ALTRI MODULI ---
    val amicoRepositoryImpl: AmicoRepository get() = amicoRepository
    val uscitaRepositoryImpl: UscitaRepository get() = uscitaRepository
    val gruppoRepositoryImpl: GruppoRepository get() = gruppoRepository

    // 2. AuthRepository (Inizializzato a runtime con il Context di Android)
    lateinit var authRepository: AuthRepository
        private set

    fun initialize(context: Context) {
        authRepository = AuthRepositoryImpl(context.applicationContext, auth)
    }

    init {
        // Avvio login anonimo di base se necessario
        if (auth.currentUser == null) {
            auth.signInAnonymously()
        }
    }
}