# 🔍 Bug Report — Armadio App (F0 Foundation)

Analisi completa del codice sorgente in `android/` confrontato con le specifiche in `prompt.md`.

---

## 🔴 Critici — Impediscono build o test

### 1. Golden Schema JSON mancante → `SchemaV1Test` fallirà

**File:** `android/app/schemas/com.armadio.core.database.AppDatabase/.gitkeep`

La directory `app/schemas/com.armadio.core.database.AppDatabase/` contiene solo un `.gitkeep`, non il file `1.json` generato da Room. Il test `SchemaV1Test` usa `MigrationTestHelper.createDatabase("...", 1)` che **richiede** il JSON golden schema per creare il database dalla definizione storica. Senza di esso, tutti e 3 i test di `SchemaV1Test` falliranno.

**Fix:** Eseguire `./gradlew assembleDebug` per generare `1.json`, poi committarlo.

---

### 2. Gradle Wrapper assente → CI e comandi `./gradlew` impossibili

**File:** `android/.github/workflows/ci.yml`

Non esistono `gradlew`, `gradlew.bat`, né `gradle/wrapper/gradle-wrapper.jar` nel progetto. Il CI esegue `./gradlew ...` e fallirà immediatamente. La DoD F0 non è raggiungibile senza il wrapper.

**Fix:** Eseguire `gradle wrapper --gradle-version 8.11.1` nella directory `android/` e committare i 4 file generati.

---

### 3. `SchemaV1Test.v1_goldenSchema_containsEveryTable` — asserzione sbagliata

**File:** `android/app/src/test/java/com/armadio/core/database/SchemaV1Test.kt`

Il test verifica che le tabelle nel DB siano **esattamente** le 10 elencate filtrando solo quelle che iniziano per `sqlite_`. Ma ci sono tabelle aggiuntive invisibili:
- `garments_fts_content`, `_segments`, `_segdir` (Shadow tables di FTS4)
- `room_master_table`
- `android_metadata`

L'`assertEquals(expected, tables)` fallirà perché `tables` conterrà ~15 entry vs le 10 attese.

**Fix:** Cambiare la logica a una verifica di **contenimento** anziché uguaglianza esatta (`assertTrue(tables.containsAll(expected))`).

---

## 🟡 Bug di codice

### 4. `Theme.kt` — import inutilizzati

**File:** `android/app/src/main/java/com/armadio/core/designsystem/Theme.kt`

`darkColorScheme` e `lightColorScheme` sono importati ma usati solo in `Color.kt`. Genererà warning del compilatore.

---

### 5. `Spacing.kt` — nuova istanza a ogni ricomposizione

**File:** `android/app/src/main/java/com/armadio/core/designsystem/Spacing.kt`

`get() = Spacing()` crea una nuova istanza a ogni accesso. 

**Fix:** Usare un singleton top-level.

---

### 6. `backup_rules.xml` — trailing slash inconsistente

**File:** `android/app/src/main/res/xml/backup_rules.xml` vs `data_extraction_rules.xml`

In `backup_rules.xml` manca il trailing slash (`path="wardrobe"`) rispetto a `data_extraction_rules.xml` (`path="wardrobe/"`). È un'inconsistenza formale.

---

### 7. Nome database `"armario.db"` vs nome app `"armadio"`

**File:** `android/app/src/main/java/com/armadio/core/database/AppDatabase.kt`

Il package è `com.armadio` ma il DB si chiama `armario.db` (in spagnolo).

---

## 🟠 Problemi di design / rischi

### 8. `Loan` con PK su `garmentId` — impedisce lo storico prestiti

**File:** `android/app/src/main/java/com/armadio/core/database/entity/TripEntities.kt`

`@PrimaryKey val garmentId: Long` impedisce di avere uno storico dei prestiti (permette solo un record di prestito per capo in tutta la vita dell'app).

---

### 9. FTS `search()` — nessuna sanitizzazione dell'input

**File:** `android/app/src/main/java/com/armadio/core/database/dao/GarmentDao.kt`

Il parametro `query` passa direttamente al `MATCH` FTS4. Caratteri speciali possono causare crash SQLite.

---

### 10. ADR 0002 incompleto

**File:** `android/docs/adr/0002-room-schema-deviations.md`

Non documenta che anche `Trip` e `Outfit` hanno ricevuto un `@PrimaryKey(autoGenerate = true)` rispetto alla specifica originale.

---

### 11. Prompt.md conteneva un errore di sintassi Kotlin

**File:** `prompt.md`

Il prompt indicava `abstract fun garmentDao(): GarmentDao()` (con parentesi finali errate). Il codice sorgente implementato è corretto, ma il prompt aveva questo piccolo refuso.
