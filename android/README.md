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
