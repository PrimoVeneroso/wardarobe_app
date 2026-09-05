# Armadio

Questo repository contiene il codice sorgente di **Armadio**, un gestore di guardaroba offline-first per Android.

> **Nota:** questo ambiente di sviluppo è orientato al web, quindi il progetto Android vero e proprio risiede nella cartella `android/`. La web app nella root serve solo da landing page di presentazione ed è buildabile con `npm run build`.

## Struttura

- `android/` — progetto Android Gradle completo (F0 foundations).
- `src/`, `index.html`, `package.json` — landing page React/Vite per presentare il progetto.

## Bug corretti rispetto al piano originale

1. **`Color.kt`**: aggiunti gli import mancanti `lightColorScheme` e `darkColorScheme` da `androidx.compose.material3`.
2. **`Type.kt`**: aggiunto l'import di `androidx.compose.material3.Typography`.
3. **`InventoryScreen.kt`**: aggiunto l'import dell'estensione `spacing` dal package `com.armadio.core.designsystem`.
4. **`app/build.gradle.kts`**: corretta la sezione `packaging` — sostituito il pattern a brace `/META-INF/{AL2.0,LGPL2.1}` con due esclusioni esplicite, compatibili con Gradle.
5. **`app/build.gradle.kts`**: resa la ricerca dei merged manifest case-insensitive e aggiunto `packaged_manifest` per robustezza su AGP 8.

## Come creare la repository GitHub

1. Entra nella cartella Android:
   ```bash
   cd android
   ```

2. Genera il Gradle wrapper (necessario una tantum):
   ```bash
   gradle wrapper --gradle-version 8.11.1
   ```

3. Esegui il primo build per generare lo schema Room JSON:
   ```bash
   ./gradlew assembleDebug
   ```

4. Committa lo schema generato:
   ```bash
   git add app/schemas/com.armadio.core.database.AppDatabase/1.json
   git commit -m "Add Room golden schema v1"
   ```

5. Inizializza la repo e pusha su GitHub:
   ```bash
   git init
   git add .
   git commit -m "F0: foundations"
   gh repo create armadio --public --source=. --push
   ```

## Comandi utili

```bash
./gradlew assembleDebug                 # APK debug
./gradlew testDebugUnitTest             # test unitari Robolectric
./gradlew checkNoNetworkPermission      # guardrail zero permessi
```

## Roadmap

- F0: fondamenta (questo commit)
- F1: inventario completo, foto, export/import ZIP
- F2: outfit, statistiche, lookbook
- F3: viaggi, prestiti, manutenzione
- F4: ML on-device
- F5: evoluzioni

Per i dettagli architetturali vedi `android/docs/adr/`.
