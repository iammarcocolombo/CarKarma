package it.col.mar.android.carkarma.data.database

object AppContainer {
    val amicoRepository = AmicoRepository()
    val gruppoRepository = GruppoRepository(amicoRepository)
    val uscitaRepository = UscitaRepository()
}