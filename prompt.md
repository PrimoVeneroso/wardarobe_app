questo era il prompt di partenza:
Ecco il prompt completo, aggiornato con la galleria `index.html` e la modalità Lookbook integrate nel formato di export, nei test e nelle DoD. Le parti nuove sono segnate nel changelog in fondo.

````markdown
# PROGETTO: "Armadio" — App Android Guardaroba Offline-First, Zero Permessi

## RUOLO E MISSIONE
Sei un senior Android engineer. Sviluppi da zero (greenfield) un'app di gestione
guardaroba il cui posizionamento è: **privacy-first, 100% offline, zero permessi**.
L'app non ha MAI accesso alla rete. Questa non è una preferenza: è la promessa
contrattuale verso l'utente e deve essere tecnicamente falsificabile (vedi CI check).

L'export non è solo un backup per l'app: deve essere **immediatamente fruibile da
un essere umano senza l'app** (galleria HTML nello ZIP) e da altre macchine
(JSON canonico). Nessun lock-in, nemmeno visivo.

Codice, identifier, commenti e commit message in inglese. UI localizzata via
resources (default inglese; aggiungi `values-it`).

## VINCOLI INVALICABILI (violano il progetto se infranti)
1. Il merged manifest NON deve contenere `android.permission.INTERNET` né alcun
   permesso runtime. Implementa un task Gradle `checkNoNetworkPermission` che
   fallisce la build se compare. Foto: Photo Picker e `TakePicture` (senza
   dichiarare CAMERA nel manifest). File: SAF (`ACTION_CREATE_DOCUMENT` /
   `ACTION_OPEN_DOCUMENT`) + FileProvider.
2. `allowBackup="false"` + `dataExtractionRules`/`fullBackupContent` che escludono
   database e immagini. Documenta in `docs/adr/0001-backup-policy.md`.
3. EXIF strippato ALL'ACQUISIZIONE (mai dopo): copia i byte immediatamente (i grant
   URI del Photo Picker sono temporanei), rimuovi metadati, poi ricomprimi.
4. Mai immagini come BLOB nel DB. Mai path assoluti nel DB: solo nome file,
   risolto a runtime su `context.filesDir/wardrobe`. Aggiungi `.nomedia`.
5. Denaro: `Int` centesimi + valuta ISO 4217. Mai Float/Double.
6. Nel DB: codici stabili (`WINTER`, `NEW`, `REPLACE`...), mai stringhe localizzate.
7. VIETATO `fallbackToDestructiveMigration()`. Room con `room.schemaLocation`,
   schema JSON committati, migrazioni testate con `MigrationTestHelper`.
8. L'import di ZIP è input ostile: anti-Zip-Slip (rifiuta path assoluti e `..`),
   anti-zip-bomb (cap su entry count e dimensione decompressa totale),
   verifica SHA-256 immagini contro manifest. Import in UNA transazione.
9. ML (solo F4): modello `.tflite` quantizzato bundled negli assets. Nessun
   ML Kit / Play Services per i modelli. NNAPI è deprecato: delegate GPU o
   XNNPACK. Fallback grato a inserimento manuale se l'inferenza è lenta.
10. Nessuna dipendenza fuori dallo stack approvato senza ADR.
11. Ogni HTML generato dall'app: CSS e JS INLINE, ZERO URL esterni, deve
    funzionare da `file://` con doppio click (niente ES modules, niente `fetch`).
    Test automatico che verifica l'assenza di `http://`, `https://`, `type="module"`
    e `fetch(` nel file generato.

## STACK (approvato, non espandere senza ADR)
- Kotlin 2.x, minSdk 26, targetSdk 35, Gradle version catalog
- Jetpack Compose + Material 3, single-activity, Navigation Compose
- Room + KSP, Coroutines/Flow, Hilt, Coil, Paging 3 (oltre ~500 item),
  DataStore Preferences, WorkManager, LiteRT (F4)

## STRUTTURA
Mono-modulo, package-by-feature: `core/` (database, images, backup, designsystem),
`feature/garments/`, `feature/outfits/`, `feature/trips/`, `feature/stats/`,
`feature/settings/`. Estrai `:core:backup` come modulo Gradle separato quando
supera ~15 file: è il codice più rischioso (security + escaping) e senza UI.

In `core/backup` i componenti chiave:
- `BackupExporter` — assembla lo ZIP in streaming
- `BackupImporter` — valida, mergea, ripristina
- `HtmlGallery` — generatore HTML con DUE modalità:
  `buildLinked(data)` → `index.html` con path relativi a `images/` (per lo ZIP)
  `buildEmbedded(data)` → singolo file HTML con thumbnail in base64 (Lookbook)
  Funzioni pure (input dati → output stringa/stream), massimamente testabili.
- Util `escapeHtml()` — gestisce contesto testo E attributo (`& < > " '`).

## MODELLO DATI (implementa esattamente questo schema Room)
```kotlin
@Entity(tableName = "garments",
  indices = [Index("uuid", unique = true), Index("categoryId"), Index("deletedAt")])
data class Garment(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val uuid: String, val name: String, val notes: String? = null,
  val imageFile: String?, val thumbFile: String?,
  val categoryId: Long, val brand: String? = null, val size: String? = null,
  val colorPrimary: Int? = null,   // ARGB
  val colorFamily: String? = null, // derivato (bianco/nero/grigio/...) per i chip filtro
  val colorSecondary: Int? = null, val pattern: String? = null,
  val fabric: String? = null, val careFlags: Int = 0,     // bitmask simboli lavaggio GINETEX
  val seasonsMask: Int, val dressCodeMask: Int,           // bitmask, non enum monovalore
  val condition: String,        // NEW | GOOD | WORN | REPLACE
  val quantity: Int = 1,        // basics: 5 calzini = 1 record
  val priceCents: Int? = null, val currency: String? = null,
  val purchaseDate: Long? = null,  // epochDay
  val createdAt: Long, val updatedAt: Long,  // merge last-write-wins
  val deletedAt: Long? = null       // soft delete
)

@Entity(tableName = "trips")
data class Trip(val id: Long = 0, val uuid: String, val name: String,
                val startDate: Long, val endDate: Long, val climate: String? = null)

@Entity(tableName = "trip_items", primaryKeys = ["tripId", "garmentId"])
data class TripItem(val tripId: Long, val garmentId: Long,
                    val plannedQty: Int = 1, val packed: Boolean = false)

@Entity(tableName = "loans")
data class Loan(val garmentId: Long, val toWhom: String, val outDate: Long,
                val expectedBack: Long? = null, val returnedAt: Long? = null)

@Entity(tableName = "outfits")
data class Outfit(val id: Long = 0, val uuid: String, val name: String,
                  val seasonsMask: Int, val dressCodeMask: Int, val rating: Int? = null,
                  val notes: String? = null,
                  val createdAt: Long, val updatedAt: Long, val deletedAt: Long? = null)

@Entity(tableName = "outfit_items", primaryKeys = ["outfitId", "garmentId"])
data class OutfitItem(val outfitId: Long, val garmentId: Long,
                      val slot: String, val sortOrder: Int)  // persisti il layout

@Entity(tableName = "wear_log")
data class WearLog(val date: Long, val outfitId: Long? = null, val garmentId: Long? = null)

@Entity(tableName = "tags") data class Tag(@PrimaryKey val id: Long, val name: String)
@Entity(tableName = "garment_tags", primaryKeys = ["garmentId", "tagId"])
data class GarmentTag(val garmentId: Long, val tagId: Long)

@Fts4(contentEntity = Garment::class)
@Entity(tableName = "garments_fts")
data class GarmentFts(val name: String, val brand: String?, val notes: String?)
```
REGOLE sul modello:
- Stati dinamici = RELAZIONI (trip_items, loans), MAI booleani sul capo.
- PK `Long` autoincrement; `uuid` solo per il merge export/import.
- Filtri via DAO con parametri nullable, MAI concatenazione SQL:
  `(:season IS NULL OR seasonsMask & :season != 0) AND deletedAt IS NULL`.
- Cancellazione capo: soft delete + Snackbar Undo + gestione outfit/trip orfani
  (avviso all'utente, non silenzio). Purge orfani via WorkManager.

## PIPELINE IMMAGINI (non negoziabile)
1. Acquisizione → copia byte → strip EXIF → ricompressa max 2048px WebP q85
   (toggle "conserva originali") → thumbnail ~512px WebP.
2. Le griglie NON decodificano mai gli originali: solo thumbnail + Coil + Paging 3.
3. Gestisci "immagine mancante": placeholder + report integrità in settings.
4. Storage pieno durante acquisizione/export: errore utente-friendly, mai crash.
5. Le thumbnail sono DERIVATE e NON vengono esportate nello ZIP: all'import
   rigenerale con la stessa pipeline dell'acquisizione (WorkManager, progress
   visibile). Un'unica fonte di verità per la generazione.

## BACKUP / EXPORT-IMPORT (formato versionato)

### Struttura ZIP
`manifest.json` + `data.json` + `images/` + `index.html` + `data.csv` (opzionale).

RUOLO DI OGNI FILE (rispetta la separazione):
- `data.json` — CANONICO per le macchine: source of truth dell'import. Se un
  utente avanzato lo modifica e re-importa, le sue modifiche contano.
- `images/` — le foto (solo originali ricompressi, nomi UUID, mai thumbnail).
- `index.html` — per gli UMANI: galleria navigabile generata a export-time.
  È un ARTEFATTO DERIVATO: l'import lo IGNORA completamente (lo ZIP importa
  anche senza) e viene ricreato a ogni export. Mai parsarlo.
- `manifest.json` — verifica integrità delle immagini. `index.html` NON è
  elencato nel manifest (derivato).
- `data.csv` — solo per l'utente umano (escaping, BOM); non canonico.

```json
{ "format": "com.armadio.backup", "formatVersion": 1, "schemaVersion": 1,
  "appVersion": "...", "createdAt": "...",
  "counts": {"garments": 240},
  "images": [{"file": "images/….webp", "sha256": "…", "garmentUuid": "…"}] }
```

### index.html — specifica della galleria
- Griglia di card raggruppata per categoria; ogni card: foto (path relativo
  `images/<file>`), nome, chip colore, badge condizione, campi chiave (tessuto,
  taglia, prezzo, stagioni, tag). Dettaglio completo in `<details>` per card.
- Capo senza foto → placeholder (div con icona), MAI `<img>` rotto.
- CSS e JS INLINE (vincolo 11). Vanilla JS ~100 righe: chip di filtro
  (categoria/stagione/condizione) + box di ricerca. La pagina resta una griglia
  completa e navigabile anche con JavaScript disattivato.
- ESCAPE HTML di ogni campo utente, sia in contesto testo sia in attributo
  (`alt`, `title`). Test: un capo con nome `<script>alert(1)</script>` deve
  apparire come testo visibile, mai eseguirsi.
- Ogni `<img>`: `loading="lazy"` + `alt` descrittivo (nome + colore).
- HTML semantico: `main`/`article`/`h2`/`dl`. Label localizzate al momento
  dell'export con le risorse dell'app.
- `@media print`: `break-inside: avoid` sulle card e intestazioni di gruppo —
  l'utente può "Stampa → Salva come PDF" e ottenere un inventario fotografico
  cartaceo (utile anche per assicurazioni) senza una riga di codice PDF.
- Genera in STREAMING su file (append progressivo), non in un'unica String in
  memoria: con 1000 capi l'HTML supera i 5 MB.

Esempio di card (riferimento, non vincolante al pixel):
```html
<article class="card" data-seasons="8" data-condition="REPLACE">
  <img src="images/3f2a9c….webp" loading="lazy" alt="Maglione blu">
  <h3>Maglione blu</h3>
  <span class="chip" style="--c:#2b4d8f">Blu</span>
  <span class="badge replace">Da sostituire</span>
  <details><dl>
    <dt>Tessuto</dt><dd>Lana 100%</dd>
    <dt>Prezzo</dt><dd>€ 89,00</dd>
  </dl></details>
</article>
```

### Lookbook (F2) — condivisione come singolo file
Voce separata ("Condividi → Lookbook"): l'utente seleziona capi e/o outfit e
l'app genera UN SINGOLO file HTML autonomo (`HtmlGallery.buildEmbedded`):
- Embedda le THUMBNAIL (`thumbFile`) come data URI base64. MAI gli originali:
  il base64 gonfia del ~33% e un guardaroba da 500 capi diventerebbe ingestibile
  (con le thumb resta sotto ~50 MB).
- Mostra la dimensione stimata PRIMA di generare; avvisa oltre i 50 MB.
- Stesse regole di escaping, lazy loading, zero risorse esterne (vincolo 11).

### Flussi
- **Export**: SAF `ACTION_CREATE_DOCUMENT` come via primaria; Share Intent con
  FileProvider come secondaria. Streaming con `ZipOutputStream`.
- **Import**: SAF `ACTION_OPEN_DOCUMENT` → staging dir → valida manifest/
  checksum/anti-Zip-Slip → migra per `formatVersion` → UNA transazione, merge
  per `uuid`, conflitti last-write-wins su `updatedAt`, rispetta le tombstone →
  copia immagini → WorkManager rigenera le thumbnail → report
  (importati/saltati/errori). `index.html` ignorato.

## SVILUPPO PER FASI — completa la Definition of Done di una fase prima della successiva

### F0 — Fondamenta
Scaffold, tema M3 (dark incluso), navigation, Room+FTS+schema export,
`checkNoNetworkPermission`, CI (build + test), README con istruzioni dev.
**DoD:** `./gradlew assembleDebug testDebugUnitTest checkNoNetworkPermission` verdi.

### F1 — MVP Inventario
CRUD capi con form completo, acquisizione foto con pipeline completa, griglia
con thumbnail + Paging, filtri (categoria/stagione/condizione/colore/dress
code), ricerca FTS, categorie e tag utente, export E import completi con
galleria `index.html`, empty states, gestione process death.
**DoD:** flusso "fotografo → salvo → filtro → esporto → disinstallo → reinstallo
→ importo → ritrovo il capo" dimostrato da test end-to-end. E: esporto lo ZIP,
lo estraggo su un PC, apro `index.html` nel browser — vedo ogni capo con la sua
foto e i suoi tag, i filtri funzionano, DevTools → Network mostra zero richieste.

### F2 — Outfit, Statistiche, Lookbook
Outfit a SLOT FISSI (testa/top/bottom/scarpe/accessori — NON canvas free-form),
metadati outfit, wear log con tap "indossato oggi", statistiche (cost-per-wear,
capi mai indossati, ultimo utilizzo, composizione guardaroba), operazioni batch.
Modalità **Lookbook** (spec sopra). Estendi `HtmlGallery.buildLinked` con una
sezione Outfit (immagini degli item affiancate) nell'`index.html`.

### F3 — Viaggi, Prestiti, Cura
Trip con checklist packed/unpacked (dati PERSISTENTI, mai tabelle TEMPORARY),
prestiti con anagrafica, eventi lavanderia con data, reminder locali via
WorkManager (permesso notifiche API 33+), "modalità lavaggio" (raggruppa capi
lavabili insieme per careFlags + colore), wishlist (capo REPLACE → item con un tap).

### F4 — ML On-Device (opzionale, degradabile)
Colore predominante: estrazione palette deterministica (median-cut su bitmap
ridotta) — NESSUNA rete neurale. Macro-categoria: MobileNetV3-Small quantizzato
`.tflite` bundled. Ogni suggerimento è proposal: l'utente accetta/rifiuta/
sovrascrive prima del salvataggio. Timeout inferenza → fallback manuale silenzioso.

### F5 — Evoluzioni
Canvas outfit free-form, blocco biometrico + SQLCipher (toggle), export cifrato
(PBKDF2 + AES-GCM, cifra l'intero ZIP incluso index.html), widget, backup
automatico periodico.

## TESTING (requisito, non opzione)
- DAO + ogni migrazione: `MigrationTestHelper` sugli schema committati.
- Backup/Import: golden-file ZIP di riferimento (ORA include index.html); ZIP
  corrotto; ZIP Zip-Slip; zip bomb; checksum mismatch; merge con conflitti uuid
  e tombstone; ZIP SENZA index.html deve importare correttamente (derivato);
  dopo import ogni capo con immagine ha thumbnail rigenerata valida.
- HTML generato: nessun URL esterno (regex su http://, https://), nessun
  `type="module"`, nessun `fetch(`; XSS — capo con nome `<script>alert(1)</script>`
  e note con `"` e `'` renderizzati come testo; ogni `<img>` ha `loading="lazy"`
  e `alt` non vuoto; Lookbook usa thumbFile, MAI imageFile (test esplicito).
- Repository con Robolectric, Flow con Turbine, UI Compose per i flussi critici
  (aggiunta capo, export, import).
- Se non puoi eseguire i test nell'ambiente corrente, scrivili comunque e
  dichiara esplicitamente cosa non hai potuto verificare. Non simulare esiti.

## NFR (verificabili)
Cold start < 1.5s; scroll 60fps con 1000+ capi; export 1000 capi (immagini +
HTML) < 30s; APK < 20 MB senza ML (+ ~4 MB con modello). Device di riferimento:
mid-range 4GB RAM. Accessibilità: TalkBack, content description descrittiva su
tutte le immagini (non "immagine" ma "maglione blu, ecc."). Nota onboarding:
avvisa che la disinstallazione cancella i dati e suggerisce l'export.

## MODALITÀ DI LAVORO
1. Parti presentando il piano della fase corrente (file che creerai, in ordine).
2. Lavora incrementalmente: piccole commit, una feature alla volta.
3. Ogni decisione architetturale ambigua → ADR in `docs/adr/NNN-*.md` (contesto,
   opzioni, decisione, conseguenze). Se l'ambiguità è bloccante, chiedi PRIMA di
   assumertela.
4. Prima di dichiarare done una fase: esegui build + test + permission check,
   e confronta il risultato con la DoD della fase.
5. Anti-patterni vietati (checklist finale di ogni fase): INTERNET permission,
   BLOB immagini, path assoluti nel DB, Float per denaro, enum localizzati nel
   DB, `fallbackToDestructiveMigration`, booleani per stati relazionali, tabelle
   TEMPORARY per dati utente, concatenazione SQL da input utente, ML Kit,
   URL esterni o risorse non-inline nell'HTML generato, parsare index.html
   in import, incorporare originali (invece delle thumb) nel Lookbook.

**Inizia ora con F0: presenta il piano dei file, poi implementa.**
````

**Changelog rispetto alla versione precedente:**

1. **Vincolo 11 (nuovo)** — HTML generato: tutto inline, zero URL esterni, funzionante da `file://`, con test automatico. Promossa a vincolo invalicabile perché è la stessa promessa offline dell'app, applicata all'export.
2. **Sezione BACKUP ristrutturata** — separazione esplicita dei ruoli: `data.json` per le macchine, `index.html` per gli umani (artefatto derivato, ignorato dall'import), specifica completa della galleria (card, escaping, lazy, print CSS, streaming) e specifica Lookbook.
3. **Pipeline immagini, punto 5 (nuovo)** — le thumbnail non si esportano, si rigenerano all'import con la stessa pipeline: una sola fonte di verità.
4. **Componenti `core/backup` nominati** — `BackupExporter`, `BackupImporter`, `HtmlGallery` (due modalità) e `escapeHtml()`, per dare all'agente confini precisi.
5. **Testing esteso** — golden file con index.html, test XSS/escaping (testo e attributi), test assenza URL esterni, test "ZIP senza index.html importa", test "Lookbook usa thumb non originali".
6. **DoD F1 aggiornata** — ora include la verifica manuale su PC (estrarre → aprire → zero richieste in Network). **F2** include Lookbook + estensione della galleria con la sezione Outfit.
7. **Anti-patterni aggiunti alla checklist** — URL esterni nell'HTML, parsare index.html, originali nel Lookbook.




Secondo prompt con istruzioni
# F0 — Fondamenta: piano, poi implementazione

## Piano dei file (ordine di creazione)

| # | File | Scopo |
|---|------|-------|
| 1 | `settings.gradle.kts`, `gradle/libs.versions.toml`, `build.gradle.kts`, `gradle.properties`, `gradle/wrapper/gradle-wrapper.properties` | Build system, version catalog con tutto lo stack approvato (le librerie delle fasi successive sono catalogate ma **non** ancora dichiarate come dipendenze) |
| 2 | `app/build.gradle.kts` | Config Android + KSP Room `schemaLocation` + task `checkNoNetworkPermission` |
| 3 | `app/src/main/AndroidManifest.xml`, `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml` | Zero permessi, `allowBackup=false`, regole difensive |
| 4 | `res/values/themes.xml` (+night), `res/values/strings.xml`, `values-it/strings.xml`, icone launcher | Tema di bootstrap, localizzazione |
| 5 | `docs/adr/0001-backup-policy.md`, `docs/adr/0002-room-schema-deviations.md` | Le due decisioni architetturali di F0 |
| 6 | `core/designsystem` | Tema M3 light+dark |
| 7 | `core/database` | **Tutte le 10 tabelle** (fissa lo schema v1 golden subito), `GarmentDao` minimale, `AppDatabase`, modulo Hilt |
| 8 | `ArmarioApplication`, `MainActivity`, `core/navigation` | Entry point, NavHost |
| 9 | `feature/garments/InventoryScreen.kt` + ViewModel | Placeholder che legge il conteggio da Room (smoke test end-to-end Hilt+Room+Compose+Flow) |
| 10 | `app/src/test/…` | DAO test (Robolectric + Turbine), schema test (`MigrationTestHelper`), `robolectric.properties` |
| 11 | `.github/workflows/ci.yml`, `README.md`, `.gitignore` | CI e onboarding dev |

## Ambiguità rilevate (nessuna bloccante per F0)

1. **`WearLog` non ha PK** — Room rifiuta entità senza chiave primaria. Aggiungo `id` surrogate autoincrement; chiave logica resta `(date, outfitId, garmentId)`. Documentato in ADR 0002.
2. **`Loan` non dichiara PK** — uso `@PrimaryKey` su `garmentId` (semanticamente: un prestito attivo per capo). In ADR 0002.
3. **`Tag.id` senza `autoGenerate`** — renderebbe l'inserzione manuale; aggiungo `autoGenerate = true` con default `0`. In ADR 0002.
4. **`POST_NOTIFICATIONS` (F3) vs vincolo 1 "zero permessi runtime"** — conflitto apparente nelle specifiche. Non blocca F0: il task ha una allowlist vuota, gateata da ADR. **Ti chiedo di confermare in F3** se i reminder devono usare notifiche (→ ADR + allowlist) o restare in-app (→ zero permessi a oltranza).

---

## 1. Build system

### `settings.gradle.kts`
```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // no rogue repo injection
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "armadio"
include(":app")
```

### `gradle/libs.versions.toml`
```toml
[versions]
agp = "8.7.3"
kotlin = "2.0.21"
ksp = "2.0.21-1.0.28"
hilt = "2.52"
hiltNavigationCompose = "1.2.0"
room = "2.6.1"
composeBom = "2024.11.00"
activityCompose = "1.9.3"
navigationCompose = "2.8.4"
lifecycle = "2.8.7"
coreKtx = "1.15.0"
coroutines = "1.9.0"
# F1+ (catalog-only until the phase that uses them)
coil = "2.7.0"
paging = "3.3.4"
datastore = "1.1.1"
work = "2.10.0"
# Test
junit = "4.13.2"
robolectric = "4.14.1"
androidxTestCore = "1.6.1"
turbine = "1.1.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
androidx-hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
room-paging = { group = "androidx.room", name = "room-paging", version.ref = "room" }              # F1
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }                # F1
paging-runtime = { group = "androidx.paging", name = "paging-runtime-ktx", version.ref = "paging" } # F1
paging-compose = { group = "androidx.paging", name = "paging-compose", version.ref = "paging" }     # F1
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" } # F1
work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "work" }     # F3
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
androidx-test-core = { group = "androidx.test", name = "test-core-ktx", version.ref = "androidxTestCore" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

### `build.gradle.kts` (root)
```kotlin
// Root build file: only plugin versions, applied per-module.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
```

### `gradle.properties`
```properties
org.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

### `gradle/wrapper/gradle-wrapper.properties`
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```
> ⚠️ **Bootstrap obbligatorio una tantum**: `gradlew` + `gradle-wrapper.jar` non posso generarli qui. Esegui una volta `gradle wrapper --gradle-version 8.11.1` e committa i 4 file generati, pena CI rossa.

---

## 2. `app/build.gradle.kts` con il guardrail permessi

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.armadio"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.armadio"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Exported Room schema JSON is committed (constraint #7):
    // app/schemas/com.armadio.core.database.AppDatabase/1.json
    sourceSets {
        getByName("test") {
            assets.srcDir("$projectDir/schemas") // feeds MigrationTestHelper in unit tests
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true // Robolectric
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    jvmToolchain(17)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

// ---------------------------------------------------------------------------
// Zero-permission guardrail (project constraint #1).
// Fails the build if any <uses-permission> reaches a merged manifest.
// The allowlist is intentionally EMPTY: adding an entry requires an ADR
// (e.g. POST_NOTIFICATIONS in F3, if we decide reminders need notifications).
// ---------------------------------------------------------------------------
val checkNoNetworkPermission = tasks.register("checkNoNetworkPermission") {
    group = "verification"
    description =
        "Fails if the merged manifest declares any <uses-permission> (network or runtime)."
    val intermediates = layout.buildDirectory.dir("intermediates")
    doLast {
        val allowedPermissions = emptySet<String>()

        val usesPermissionTag = Regex(
            """<uses-permission(?:-sdk-\d+)?\b[^>]*>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        val androidName = Regex("""android:name\s*=\s*"([^"]+)"""")

        // Covers AGP 7 ("merged_manifests/<variant>/AndroidManifest.xml") and
        // AGP 8 ("merged_manifest/<variant>/process*MainManifest/merged.xml").
        val mergedManifests = intermediates.get().asFile
            .walkTopDown()
            .filter { file ->
                file.isFile &&
                    file.extension.equals("xml", ignoreCase = true) &&
                    (file.absolutePath.contains("merged_manifest") ||
                        file.absolutePath.contains("packaged_manifest"))
            }
            .toList()

        if (mergedManifests.isEmpty()) {
            logger.lifecycle(
                "checkNoNetworkPermission: no merged manifests found yet, nothing to verify. " +
                    "(The task runs automatically after process*MainManifest.)"
            )
            return@doLast
        }

        val violations = mergedManifests
            .flatMap { file -> usesPermissionTag.findAll(file.readText()) }
            .mapNotNull { tag -> androidName.find(tag.value)?.groupValues?.get(1) }
            .filter { permission -> permission !in allowedPermissions }
            .distinct()
            .sorted()

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Armadio is a ZERO-PERMISSION app (project constraint #1). The merged " +
                    "manifest declares permission(s) not on the ADR-gated allowlist: " +
                    "${violations.joinToString(", ")}. Remove the dependency/feature that " +
                    "pulls them in, or write an ADR and extend the allowlist."
            )
        }
        logger.lifecycle(
            "checkNoNetworkPermission: OK — 0 permissions across ${mergedManifests.size} merged manifest(s)."
        )
    }
}

// Run the guardrail right after each variant's manifest is merged, so a bad
// dependency fails as early as possible. Both assembleDebug and
// testDebugUnitTest trigger manifest processing, so the check covers them.
listOf("processDebugMainManifest", "processReleaseMainManifest").forEach { taskName ->
    tasks.matching { it.name == taskName }.configureEach {
        finalizedBy(checkNoNetworkPermission)
    }
}
```

### `app/proguard-rules.pro`
```proguard
# Hilt, Room and Compose ship consumer keep rules. Nothing needed yet.
# Any future rule must be justified by a failing release build, not cargo cult.
```

---

## 3. Manifest e regole backup

### `app/src/main/AndroidManifest.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- No <uses-permission> here, ever. Enforced by checkNoNetworkPermission. -->

    <application
        android:name=".ArmarioApplication"
        android:allowBackup="false"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.Armadio">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- FileProvider + SAF flows land in F1. -->
    </application>
</manifest>
```

### `app/src/main/res/xml/backup_rules.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- API < 31 path. Belt-and-suspenders: allowBackup=false already disables
     Android Auto Backup entirely; these rules exist so that flipping
     allowBackup by accident, or OEM transfer tooling, still leaks nothing.
     See docs/adr/0001-backup-policy.md -->
<full-backup-content>
    <exclude domain="database" path="." />
    <exclude domain="file" path="wardrobe" />
    <exclude domain="file" path="datastore" />
    <exclude domain="sharedpref" path="." />
</full-backup-content>
```

### `app/src/main/res/xml/data_extraction_rules.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- API 31+ cloud backup and device-to-device transfer. See ADR 0001. -->
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="database" path="." />
        <exclude domain="file" path="wardrobe/" />
        <exclude domain="file" path="datastore/" />
        <exclude domain="sharedpref" path="." />
    </cloud-backup>
    <device-transfer>
        <exclude domain="database" path="." />
        <exclude domain="file" path="wardrobe/" />
        <exclude domain="file" path="datastore/" />
        <exclude domain="sharedpref" path="." />
    </device-transfer>
</data-extraction-rules>
```

### `app/src/main/res/values/themes.xml` e `values-night/themes.xml`
```xml
<!-- values/themes.xml -->
<resources>
    <!-- Compose owns real theming; this only avoids a flash of the wrong
         system theme before the first frame is composed. -->
    <style name="Theme.Armadio" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```
```xml
<!-- values-night/themes.xml -->
<resources>
    <style name="Theme.Armadio" parent="android:Theme.Material.NoActionBar" />
</resources>
```

### Stringhe — `values/strings.xml`
```xml
<resources>
    <string name="app_name">Armadio</string>
    <string name="inventory_title">Wardrobe</string>
    <plurals name="inventory_count">
        <item quantity="one">%1$d garment</item>
        <item quantity="other">%1$d garments</item>
    </plurals>
    <string name="inventory_f1_placeholder">Full inventory features arrive in F1.\nThe wardrobe database is already live: this counter is read from Room.</string>
</resources>
```

### `values-it/strings.xml`
```xml
<resources>
    <string name="app_name">Armadio</string>
    <string name="inventory_title">Guardaroba</string>
    <plurals name="inventory_count">
        <item quantity="one">%1$d capo</item>
        <item quantity="other">%1$d capi</item>
    </plurals>
    <string name="inventory_f1_placeholder">Le funzioni complete dell\'inventario arrivano in F1.\nIl database del guardaroba è già attivo: questo contatore è letto da Room.</string>
</resources>
```

### Icona launcher (segnaposto, minSdk 26 → solo adaptive icon)

`res/values/colors.xml`
```xml
<resources>
    <color name="ic_launcher_background">#3A6A4B</color>
</resources>
```
`res/drawable/ic_launcher_foreground.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <!-- Placeholder hanger + rack glyph, replaced by the design pass in F1. -->
    <path android:fillColor="#FFFFFFFF"
        android:pathData="M54,26c-2.8,0 -5,2.2 -5,5c0,1.7 0.8,3.1 2.1,4L30.8,49.9c-1.7,1.4 -0.7,4.1 1.5,4.1h43.4c2.2,0 3.2,-2.8 1.5,-4.1L56.9,35c1.3,-0.9 2.1,-2.4 2.1,-4C59,28.2 56.8,26 54,26z"/>
    <path android:fillColor="#B3FFFFFF"
        android:pathData="M32,62h44v3h-44z M32,69h44v3h-44z M32,76h44v3h-44z"/>
</vector>
```
`res/mipmap-anydpi-v26/ic_launcher.xml`
```xml
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

---

## 4. ADR

### `docs/adr/0001-backup-policy.md`
```markdown
# ADR 0001 — Backup policy: nothing leaves the device uninvited

## Status
Accepted (F0)

## Context
Armadio's contractual promise is privacy-first, 100% offline, zero permissions.
The database and the wardrobe photos are the most sensitive data in the app.
Android Auto Backup / device transfer would upload an unencrypted copy of both
to Google's cloud (or hand it to a transfer tool), silently breaking that promise.

## Options considered
1. `allowBackup="true"` + exclusion rules for database and images.
2. `allowBackup="false"` + defensive exclusion rules.

## Decision
Option 2. `allowBackup="false"` disables OS-managed backup and transfer entirely.
`dataExtractionRules` (API 31+) and `fullBackupContent` (API < 31) are declared
anyway, excluding database, `filesDir/wardrobe`, DataStore and shared prefs:
defense in depth against a future accidental flip of `allowBackup` or OEM
tooling that ignores the flag.

## Consequences
- No automatic restore on reinstall or device switch — by design.
- The ONLY supported backup path is the user-driven export ZIP (F1).
- Onboarding must warn: "uninstalling deletes your data — export first" (F1).
- Lint warnings about rules declared alongside `allowBackup=false` are accepted
  and documented here.
```

### `docs/adr/0002-room-schema-deviations.md`
```markdown
# ADR 0002 — Minimal deviations from the specified Room schema

## Status
Accepted (F0)

## Context
The spec says "implement exactly this schema", but Room has hard requirements
the spec text cannot satisfy as written.

## Decision
Keep every table, column, index and bitmask exactly as specified, with three
minimal, purely additive/technical deviations:

1. **WearLog** gets a surrogate `@PrimaryKey(autoGenerate=true) id: Long = 0`
   plus indices on `date` and `garmentId` (stats queries). The logical key
   remains `(date, outfitId, garmentId)`; export/import (F1) must treat
   wear_log as an append-only log keyed on that triple, never on `id`.
2. **Tag** PK becomes `autoGenerate = true` with default `0` (spec showed a
   bare Long PK; without autoGenerate every insert needs a hand-rolled id).
3. **Loan** declares `@PrimaryKey` on `garmentId` — one loan row per garment,
   matching the relational rule "dynamic state lives in relations".

## Consequences
- Schema v1 golden (app/schemas) includes the deviations from day one, so
  migrations never need to reconcile them.
- data.json (F1) serializes WearLog without the surrogate id.
- Adding DAOs for trips/outfits/tags in F2/F3 does NOT change the schema
  (no migration needed); adding tables or columns does.
```

---

## 5. `core/designsystem`

### `core/designsystem/Color.kt`
```kotlin
package com.armadio.core.designsystem

import androidx.compose.ui.graphics.Color

// Sage-and-ink palette; refined during the F1 design pass.

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3A6A4B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBCF0C6),
    onPrimaryContainer = Color(0xFF00210D),
    secondary = Color(0xFF506356),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD3E8D6),
    onSecondaryContainer = Color(0xFF0D1F12),
    tertiary = Color(0xFF815542),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF7FAF4),
    onBackground = Color(0xFF171B17),
    surface = Color(0xFFF7FAF4),
    onSurface = Color(0xFF171B17),
    surfaceVariant = Color(0xFFDCE5DB),
    onSurfaceVariant = Color(0xFF404943),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA1D4AB),
    onPrimary = Color(0xFF003920),
    primaryContainer = Color(0xFF205131),
    onPrimaryContainer = Color(0xFFBCF0C6),
    secondary = Color(0xFFB7CCB9),
    onSecondary = Color(0xFF223527),
    secondaryContainer = Color(0xFF394B3D),
    onSecondaryContainer = Color(0xFFD3E8D6),
    tertiary = Color(0xFFF3B9A5),
    onTertiary = Color(0xFF4A2616),
    background = Color(0xFF0F120F),
    onBackground = Color(0xFFDEE4DC),
    surface = Color(0xFF0F120F),
    onSurface = Color(0xFFDEE4DC),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFC0C9C0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)
```

### `core/designsystem/Type.kt`
```kotlin
package com.armadio.core.designsystem

import androidx.compose.material3.Typography

// M3 defaults; custom type scale lands with the F1 design pass.
val ArmarioTypography = Typography()
```

### `core/designsystem/Theme.kt`
```kotlin
package com.armadio.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun ArmarioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Material You is opt-in; the brand palette is the default look.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ArmarioTypography,
        content = content,
    )
}
```

---

## 6. `core/database` — schema completo v1

### `core/database/entity/Garment.kt`
```kotlin
package com.armadio.core.database.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A wardrobe item.
 *
 * Rules encoded here (project constraints):
 * - Dynamic state (packed, loaned out, worn) lives in relational tables
 *   (trip_items, loans, wear_log) — NEVER in boolean columns on this entity.
 * - [imageFile]/[thumbFile] are file NAMES only, resolved at runtime against
 *   context.filesDir/wardrobe. No absolute paths in the DB, no BLOBs.
 * - Money is [priceCents] (minor units) + [currency] (ISO 4217). Never Float.
 * - [condition], [colorFamily], etc. are stable codes, never localized text.
 * - [deletedAt] is a tombstone for soft delete + merge (last-write-wins on
 *   [updatedAt] during import).
 */
@Entity(
    tableName = "garments",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["categoryId"]),
        Index(value = ["deletedAt"]),
    ],
)
data class Garment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val name: String,
    val notes: String? = null,
    val imageFile: String? = null,
    val thumbFile: String? = null,
    val categoryId: Long,
    val brand: String? = null,
    val size: String? = null,
    val colorPrimary: Int? = null,   // ARGB
    val colorFamily: String? = null, // derived (white/black/gray/...) for filter chips
    val colorSecondary: Int? = null,
    val pattern: String? = null,
    val fabric: String? = null,
    val careFlags: Int = 0,          // bitmask of GINETEX care symbols
    val seasonsMask: Int,            // bitmask, never a mono-value enum
    val dressCodeMask: Int,          // bitmask
    val condition: String,           // NEW | GOOD | WORN | REPLACE
    val quantity: Int = 1,           // basics: 5 identical socks = 1 record
    val priceCents: Int? = null,
    val currency: String? = null,
    val purchaseDate: Long? = null,  // epoch day
    val createdAt: Long,             // epoch millis
    val updatedAt: Long,             // epoch millis, merge last-write-wins
    val deletedAt: Long? = null,     // epoch millis of the soft delete
)

/** FTS mirror of [Garment]; Room keeps it in sync via generated triggers. */
@Fts4(contentEntity = Garment::class)
@Entity(tableName = "garments_fts")
data class GarmentFts(
    val name: String,
    val brand: String?,
    val notes: String?,
)
```

### `core/database/entity/TripEntities.kt`
```kotlin
package com.armadio.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val name: String,
    val startDate: Long, // epoch day
    val endDate: Long,
    val climate: String? = null, // stable code (COLD | MILD | HOT), not localized
)

/**
 * Packing checklist entries. PERSISTENT user data — never a TEMPORARY table
 * (project anti-pattern list). Packing state is relational state on this
 * entity, not a boolean on Garment.
 */
@Entity(tableName = "trip_items", primaryKeys = ["tripId", "garmentId"])
data class TripItem(
    val tripId: Long,
    val garmentId: Long,
    val plannedQty: Int = 1,
    val packed: Boolean = false,
)

/** Loan ledger — one row per garment while it is out (ADR 0002). */
@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey val garmentId: Long,
    val toWhom: String,
    val outDate: Long,          // epoch day
    val expectedBack: Long? = null,
    val returnedAt: Long? = null,
)
```

### `core/database/entity/OutfitEntities.kt`
```kotlin
package com.armadio.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outfits")
data class Outfit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val name: String,
    val seasonsMask: Int,
    val dressCodeMask: Int,
    val rating: Int? = null,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null, // tombstone, same semantics as Garment
)

/** Fixed-slot layout, persisted (F2: TOP | BOTTOM | SHOES | ... stable codes). */
@Entity(tableName = "outfit_items", primaryKeys = ["outfitId", "garmentId"])
data class OutfitItem(
    val outfitId: Long,
    val garmentId: Long,
    val slot: String,
    val sortOrder: Int,
)
```

### `core/database/entity/WearLog.kt`
```kotlin
package com.armadio.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One "worn today" event per (day, outfit or garment).
 * Deviation per ADR 0002: surrogate autoincrement id because Room requires a
 * PK; the logical key used by stats and by future export/import is
 * (date, outfitId, garmentId).
 */
@Entity(
    tableName = "wear_log",
    indices = [Index("date"), Index("garmentId")],
)
data class WearLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,           // epoch day
    val outfitId: Long? = null,
    val garmentId: Long? = null,
)
```

### `core/database/entity/TagEntities.kt`
```kotlin
package com.armadio.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // ADR 0002
    val name: String,
)

@Entity(tableName = "garment_tags", primaryKeys = ["garmentId", "tagId"])
data class GarmentTag(
    val garmentId: Long,
    val tagId: Long,
)
```

### `core/database/dao/GarmentDao.kt`
```kotlin
package com.armadio.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.armadio.core.database.entity.Garment
import kotlinx.coroutines.flow.Flow

/**
 * F0 scope: insert, soft delete, counts, FTS search — enough to smoke-test
 * Room + Flow end to end. Filtering by nullable params (the
 * `(:x IS NULL OR ...)` pattern) and Paging arrive in F1.
 *
 * Note for F1: user input fed to MATCH must be sanitized (quoted tokens,
 * wildcard escaping) — never concatenated raw (project anti-pattern).
 */
@Dao
interface GarmentDao {

    @Insert
    suspend fun insert(garment: Garment): Long

    @Query("UPDATE garments SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long)

    @Query("SELECT COUNT(*) FROM garments WHERE deletedAt IS NULL")
    fun activeCount(): Flow<Int>

    @Query("SELECT * FROM garments WHERE id = :id")
    suspend fun byId(id: Long): Garment?

    @Query(
        """
        SELECT garments.* FROM garments
        JOIN garments_fts ON garments.id = garments_fts.docid
        WHERE garments_fts MATCH :query
          AND garments.deletedAt IS NULL
        ORDER BY garments.name
        """
    )
    fun search(query: String): Flow<List<Garment>>
}
```

### `core/database/AppDatabase.kt`
```kotlin
package com.armadio.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.armadio.core.database.dao.GarmentDao
import com.armadio.core.database.entity.Garment
import com.armadio.core.database.entity.GarmentFts
import com.armadio.core.database.entity.GarmentTag
import com.armadio.core.database.entity.Loan
import com.armadio.core.database.entity.Outfit
import com.armadio.core.database.entity.OutfitItem
import com.armadio.core.database.entity.Tag
import com.armadio.core.database.entity.Trip
import com.armadio.core.database.entity.TripItem
import com.armadio.core.database.entity.WearLog

/**
 * Schema v1 — the full data model is declared up front so the golden schema
 * JSON is stable from day one. Adding DAOs does not change the schema; adding
 * tables/columns requires a Migration + MigrationTestHelper test
 * (fallbackToDestructiveMigration is FORBIDDEN — constraint #7).
 */
@Database(
    entities = [
        Garment::class,
        GarmentFts::class,
        Trip::class,
        TripItem::class,
        Loan::class,
        Outfit::class,
        OutfitItem::class,
        WearLog::class,
        Tag::class,
        GarmentTag::class,
    ],
    version = 1,
    exportSchema = true, // app/schemas is committed
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun garmentDao(): GarmentDao()
    // trip/outfit/tag DAOs land with their features (F2/F3).

    companion object {
        const val NAME = "armario.db"
    }
}
```

### `core/database/di/DatabaseModule.kt`
```kotlin
package com.armadio.core.database.di

import android.content.Context
import androidx.room.Room
import com.armadio.core.database.AppDatabase
import com.armadio.core.database.dao.GarmentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            // Deliberately NO fallbackToDestructiveMigration() — constraint #7.
            // Migrations are mandatory and tested with MigrationTestHelper.
            .build()

    @Provides
    fun provideGarmentDao(database: AppDatabase): GarmentDao = database.garmentDao()
}
```

---

## 7. Entry point e navigation

### `ArmarioApplication.kt`
```kotlin
package com.armadio

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ArmarioApplication : Application()
```

### `MainActivity.kt`
```kotlin
package com.armadio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.armadio.core.designsystem.ArmarioTheme
import com.armadio.core.navigation.ArmarioApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArmarioTheme {
                ArmarioApp()
            }
        }
    }
}
```

### `core/navigation/ArmarioApp.kt`
```kotlin
package com.armadio.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.armadio.feature.garments.InventoryRoute

object Routes {
    const val INVENTORY = "inventory"
    // F1: garment detail ("garment/{id}"), categories, settings…
}

@Composable
fun ArmarioApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.INVENTORY) {
        composable(Routes.INVENTORY) { InventoryRoute() }
    }
}
```

---

## 8. `feature/garments` — smoke-test UI

### `feature/garments/InventoryViewModel.kt`
```kotlin
package com.armadio.feature.garments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armadio.core.database.dao.GarmentDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class InventoryViewModel @Inject constructor(
    garmentDao: GarmentDao,
) : ViewModel() {
    val activeCount: StateFlow<Int> = garmentDao.activeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
```

### `feature/garments/InventoryScreen.kt`
```kotlin
package com.armadio.feature.garments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armadio.R

@Composable
fun InventoryRoute(viewModel: InventoryViewModel = hiltViewModel()) {
    val activeCount by viewModel.activeCount.collectAsStateWithLifecycle()
    InventoryScreen(activeCount = activeCount)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(activeCount: Int) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.inventory_title)) }) },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            ) {
                Text(
                    text = pluralStringResource(R.plurals.inventory_count, activeCount, activeCount),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.inventory_f1_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
```

> `MaterialTheme.spacing` richiede un'extension minima — aggiungo `core/designsystem/Spacing.kt`:
```kotlin
package com.armadio.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val MaterialTheme.spacing: Spacing
    @Composable @ReadOnlyComposable get() = Spacing()

class Spacing(
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
)
```

---

## 9. Test

### `app/src/test/resources/robolectric.properties`
```properties
sdk=34
```

### `app/src/test/java/com/armadio/core/database/dao/GarmentDaoTest.kt`
```kotlin
package com.armadio.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.armadio.core.database.AppDatabase
import com.armadio.core.database.entity.Garment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GarmentDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: GarmentDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.garmentDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private var seq = 0

    private fun garment(name: String, brand: String? = null, notes: String? = null): Garment {
        seq++
        return Garment(
            uuid = "uuid-$seq-$name",
            name = name,
            notes = notes,
            brand = brand,
            categoryId = 1L,
            seasonsMask = 0b1111,
            dressCodeMask = 0b0001,
            condition = "NEW",
            createdAt = 1_000L,
            updatedAt = 1_000L,
        )
    }

    @Test
    fun insert_then_activeCount_isOne() = runTest {
        dao.insert(garment("Wool coat"))
        assertEquals(1, dao.activeCount().first())
    }

    @Test
    fun softDelete_hidesFromActiveCount_andFlowReemits() = runTest {
        val id = dao.insert(garment("Wool coat"))
        dao.activeCount().test {
            assertEquals(1, awaitItem())
            dao.softDelete(id, timestamp = 2_000L)
            assertEquals(0, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun softDelete_isATombstone_rowIsStillThere() = runTest {
        val id = dao.insert(garment("Wool coat"))
        dao.softDelete(id, timestamp = 2_000L)
        val row = dao.byId(id)
        assertNotNull(row)
        assertEquals(2_000L, row!!.deletedAt)
    }

    @Test
    fun ftsSearch_matchesName() = runTest {
        dao.insert(garment("Cashmere sweater", brand = "Acme", notes = "birthday gift"))
        dao.insert(garment("Linen shirt", brand = "Zeta"))
        val hits = dao.search("cashmere").first()
        assertEquals(listOf("Cashmere sweater"), hits.map { it.name })
    }

    @Test
    fun ftsSearch_matchesBrand_withNullNotes() = runTest {
        // Guards the nullable columns in garments_fts (brand/notes).
        dao.insert(garment("Linen shirt", brand = "Zeta", notes = null))
        assertEquals(1, dao.search("zeta").first().size)
    }

    @Test
    fun ftsSearch_excludesSoftDeleted() = runTest {
        val id = dao.insert(garment("Cashmere sweater"))
        dao.softDelete(id, timestamp = 2_000L)
        assertEquals(0, dao.search("cashmere").first().size)
    }
}
```

### `app/src/test/java/com/armadio/core/database/SchemaV1Test.kt`
```kotlin
package com.armadio.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.testing.MigrationTestHelper
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Guards the golden schema JSON in app/schemas (constraint #7). Every future
 * schema change must extend this file with MigrationTestHelper tests —
 * destructive migrations are forbidden.
 */
@RunWith(RobolectricTestRunner::class)
class SchemaV1Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun v1_goldenSchema_containsEveryTable() {
        val expected = setOf(
            "garments", "garments_fts", "trips", "trip_items", "loans",
            "outfits", "outfit_items", "wear_log", "tags", "garment_tags",
        )
        helper.createDatabase("schema-tables.db", 1).use { db ->
            val tables = mutableSetOf<String>()
            db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
                while (cursor.moveToNext()) tables.add(cursor.getString(0))
            }
            tables.removeAll { it.startsWith("sqlite_") }
            assertEquals(expected, tables)
        }
    }

    @Test
    fun v1_goldenSchema_isSelfConsistent() {
        helper.createDatabase("schema-consistency.db", 1).close()
        helper.runMigrationsAndValidate("schema-consistency.db", 1, true)
    }

    @Test
    fun v1_roomRuntimeSchema_matchesGoldenSchemaJson() {
        // Creates the db through Room's own builder, then validates the file
        // against the committed golden JSON: any drift between code and
        // schema fails here.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.databaseBuilder(context, AppDatabase::class.java, "schema-runtime.db")
            .allowMainThreadQueries()
            .build()
        db.openHelper.writableDatabase // force schema creation
        db.close()
        helper.runMigrationsAndValidate("schema-runtime.db", 1, true)
    }
}
```

---

## 10. CI e README

### `.github/workflows/ci.yml`
```yaml
name: ci

on:
  push:
    branches: [ main ]
  pull_request:

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Set up Gradle (with cache)
        uses: gradle/actions/setup-gradle@v4

      # Order matters: assembleDebug processes the manifest, which auto-runs
      # checkNoNetworkPermission (finalizedBy); the explicit invocation at the
      # end makes the guardrail visible in the log and in the DoD command.
      - name: Build, unit tests, zero-permission check
        run: ./gradlew assembleDebug testDebugUnitTest checkNoNetworkPermission --stacktrace

      - name: Upload Room schemas
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: room-schemas
          path: app/schemas/
```

### `README.md`
```markdown
# Armadio

Offline-first wardrobe manager for Android. **Privacy-first, 100% offline,
zero permissions**: the app never has network access — enforced in CI by the
`checkNoNetworkPermission` Gradle task, which fails the build if any
`<uses-permission>` reaches the merged manifest.

The only way data leaves the device is the user-driven export ZIP
(`data.json` canonical + `images/` + human-browsable `index.html` gallery),
implemented in F1.

## Non-negotiables
1. No INTERNET / no runtime permissions in the merged manifest.
2. `allowBackup=false` + exclusion rules (ADR 0001). No cloud backup, ever.
3. EXIF stripped at acquisition; recompressed WebP in `filesDir/wardrobe`.
4. No image BLOBs, no absolute paths in the DB (file names only).
5. Money = Int cents + ISO 4217 code.
6. DB stores stable codes, never localized strings.
7. No destructive migrations: schema JSON committed in `app/schemas/`,
   every migration tested with `MigrationTestHelper` (ADR 0002).
8. Import treats ZIPs as hostile input (Zip-Slip, zip bombs, checksums).
9. Generated HTML: inline CSS/JS only, works from `file://`, zero requests.

## Bootstrap
- JDK 17, Android SDK 35.
- One-time (wrapper JAR is not committed via this doc):
  `gradle wrapper --gradle-version 8.11.1` then commit `gradlew`,
  `gradlew.bat`, `gradle/wrapper/*`.
- First build generates `app/schemas/com.armadio.core.database.AppDatabase/1.json`
  — commit it (golden schema, constraint #7).

## Commands
```bash
./gradlew assembleDebug                 # debug APK
./gradlew testDebugUnitTest             # Robolectric unit tests
./gradlew checkNoNetworkPermission      # zero-permission guardrail
```

Note: Robolectric downloads `android-all` jars on the first test run —
build-time network only. The shipped app has none.

## Layout
```
app/src/main/java/com/armadio/
  core/        designsystem, database, navigation
  feature/     garments, outfits, trips, stats, settings (per phase)
docs/adr/      architecture decision records
app/schemas/   Room golden schemas (committed)
```

## ADR index
- 0001 — Backup policy: nothing leaves the device uninvited
- 0002 — Minimal deviations from the specified Room schema

## Roadmap
F0 foundations → F1 inventory+photo pipeline+export/import → F2 outfits,
stats, lookbook → F3 trips/loans/care → F4 on-device ML (degradable) → F5
evolutions. Each phase has a Definition of Done checked in CI.
```

### `.gitignore`
```
*.iml
.gradle/
.idea/
.kotlin/
build/
local.properties
captures/
.externalNativeBuild/
.cxx/
.DS_Store
```

---

## Cosa NON ho potuto verificare (ambiente senza toolchain Android)

Conformemente alle istruzioni: non dichiaro la DoD raggiunta. La DoD F0 (`assembleDebug testDebugUnitTest checkNoNetworkPermission` verdi) va eseguita sul tuo ambiente; i punti di attrito che mi aspetto, in ordine di probabilità:

1. **Path dei merged manifest** in AGP 8.7.3: il task scandisce ricorsivamente qualsiasi `merged_manifest*`/`packaged_manifest*` sotto `intermediates`, ma se un giorno AGP rinominasse le directory il task passerebbe al ramo "skipped" — follow-up: aggiungere la scansione del binario dentro l'APK come seconda linea di difesa (issue aperta).
2. **Colonne nullable in `GarmentFts`**: la spec le prevede, Room le accetta in generale; se il compilatore Room o i trigger FTS protestassero, fix = `COALESCE`-style default `""` + nota in ADR 0002. Il test `ftsSearch_matchesBrand_withNullNotes` copre il caso.
3. **`MigrationTestHelper` dentro unit test Robolectric** (anziché `androidTest`): pattern noto e funzionante grazie a `assets.srcDir("schemas")` sul source set `test`, ma è il test più sensibile al setup.
4. **Wrapper jar** da generare (vedi README Bootstrap).
5. **Lint warning** `fullBackupContent`/`dataExtractionRules` insieme a `allowBackup=false`: attesi e accettati (ADR 0001), non bloccano la build.
6. Versioni del catalog (AGP 8.7.3 / KSP `2.0.21-1.0.28` / Hilt 2.52): verificate come compatibili a coppie, non eseguite insieme qui.

**Non testati in F0 e dichiarati rimandati**: UI test Compose (solo quando c'è UI reale, F1), test del task Gradle via TestKit, onboarding "disinstalla = perdi i dati" (F1).

---

## Prossimo passo

Quando la DoD F0 è verde sul tuo ambiente (o mi segnali gli errori della prima esecuzione e li fixo), entro in **F1 — MVP Inventario** con questo ordine: pipeline immagini (acquisizione → strip EXIF → WebP 2048/q85 + thumb 512, `Photo Picker`/`TakePicture` senza permessi) → form capo completo → griglia con Paging 3 + Coil → filtri DAO nullable → ricerca FTS → `:core:backup` (Exporter/Importer/HtmlGallery + test ostili) → flusso end-to-end della DoD. Prima di iniziare F3 ti chiederò la decisione su `POST_NOTIFICATIONS` vs zero-permessi, come da ambiguità n. 4.
