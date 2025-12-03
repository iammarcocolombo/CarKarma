🚗 CarKarma

CarKarma è un'applicazione Android nativa sviluppata in Kotlin e Jetpack Compose che risolve l'eterno dilemma tra gruppi di amici: "Chi guida stasera?".

A differenza delle classiche app di split-cost, CarKarma utilizza un algoritmo equo ponderato che tiene conto non solo dei chilometri percorsi, ma anche della frequenza di partecipazione e della capacità del veicolo, garantendo che il peso della guida sia distribuito giustamente nel tempo.

(Sostituisci questo link con uno screenshot reale della tua app)

✨ Funzionalità Principali

Gestione Gruppi Cloud: I gruppi sono sincronizzati in tempo reale su Firebase Firestore.

Algoritmo "Karma" Intelligente:

Calcola il debito/credito di km per ogni utente.

Considera i posti auto disponibili (es. esclude chi ha un'auto da 2 posti se il gruppo è di 4).

Gestisce automaticamente uscite numerose suggerendo più guidatori se necessario.

Calcolo Automatico Percorsi: Integrazione con OpenRouteService per calcolare i km tra due indirizzi automaticamente.

Multi-Utente & Condivisione:

Login tramite Google o Email/Password.

Invito ai gruppi tramite Link profondo (carkarma://join/...) o QR Code.

Sincronizzazione automatica della rubrica amici quando ci si unisce a un gruppo.

Interfaccia Moderna: UI completa in Material Design 3 con supporto Dark/Light mode.

🛠️ Tech Stack

Il progetto segue le linee guida moderne dell'architettura Android:

Linguaggio: Kotlin

UI: Jetpack Compose (Single Activity)

Architettura: MVVM (Model-View-ViewModel) con Clean Architecture (Data, Domain, Presentation layers)

Navigazione: Jetpack Navigation Compose

Backend & Database: Firebase Authentication, Cloud Firestore

API Esterne: OpenRouteService (per geocoding e routing)

Concorrenza: Kotlin Coroutines & Flow

Altro: ZXing (QR Code generation)

🚀 Configurazione per lo Sviluppo

Per compilare ed eseguire questo progetto, è necessario configurare le chiavi API.

1. Firebase

Il file google-services.json NON è incluso nella repository per sicurezza.

Crea un progetto su Firebase Console.

Aggiungi un'app Android con package name: it.col.mar.android.carkarma.

Abilita Authentication (Google e Email/Password).

Crea un database Firestore e imposta le regole di sicurezza.

Scarica google-services.json e posizionalo nella cartella app/.

2. OpenRouteService (Mappe)

Ottieni una chiave API gratuita su openrouteservice.org.

Apri presentation/uscita/UscitaViewModel.kt.

Inserisci la chiave nella variabile ORS_API_KEY.

3. Login Google

Per far funzionare il login Google in debug:

Ottieni lo SHA-1 del tuo certificato di debug (./gradlew signingReport).

Aggiungilo nelle impostazioni del progetto Firebase.

Copia il Web Client ID da Firebase Authentication e incollalo in util/GoogleAuthClient.kt.

📂 Struttura del Progetto

it.col.mar.android.carkarma
├── data
│   ├── database    # Repository e AppContainer (Dependency Injection manuale)
│   └── model       # Data classes (Amico, Gruppo, Uscita)
├── domain          # Business Logic (CalcoloTurnoUseCase)
├── presentation
│   ├── amico       # Schermate gestione amici
│   ├── gruppo      # Dettaglio e modifica gruppo
│   ├── home        # Dashboard principale
│   ├── login       # Schermate di accesso
│   ├── uscita      # Gestione viaggi e calcolo percorso
│   └── navigation  # NavHost e gestione rotte
├── ui.theme        # Colori, Font e Tema Material3
└── util            # Classi helper (AuthClient, QrGenerator)


📝 Note sull'Algoritmo

L'algoritmo di selezione del guidatore (CalcoloTurnoUseCase) si basa sulla formula:
Karma = KmGuidati - (PresenzeTotali * CostoMedioPerPresenza)

L'app ordina i partecipanti presenti in base al Karma crescente (chi ha il valore più basso/negativo è il più "in debito"). Se il gruppo supera la capienza dell'auto del primo candidato, l'algoritmo scala al successivo o suggerisce più auto.

Creato da Marco Colombo