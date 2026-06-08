package it.col.mar.android.carkarma.data.database

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import it.col.mar.android.carkarma.domain.repository.AmicoRepository
import it.col.mar.android.carkarma.domain.repository.AuthRepository
import it.col.mar.android.carkarma.domain.repository.CarburanteRepository
import it.col.mar.android.carkarma.domain.repository.GruppoRepository
import it.col.mar.android.carkarma.domain.repository.UscitaRepository

object AppContainer {
    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = Firebase.auth
    private val storage: FirebaseStorage = Firebase.storage

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

    lateinit var authRepository: AuthRepository
        private set

    fun initialize(context: Context) {
        authRepository = AuthRepositoryImpl(context.applicationContext, auth)
    }

    init {
        if (auth.currentUser == null) {
            auth.signInAnonymously()
        }
    }
}