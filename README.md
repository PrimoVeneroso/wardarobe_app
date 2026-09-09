# Armadio

The native Android app is in `android-native/` (Kotlin, Jetpack Compose, Room).
Features: add, search, edit, delete clothes (name, brand, size, notes). No permissions, offline only.
Photos, outfits, trips, and export/import are planned features.

`src/` contains only the presentation webpage. Do not build with Capacitor.
The `.github/workflows/android-build.yml` workflow builds the native app.

## Local Build

Requirements: JDK 17, Android SDK 35, Build Tools 34.0.0. Set `sdk.dir` in `android-native/local.properties`.

```bash
bash scripts/create-signing-key.sh # First time only
cd android-native
./gradlew assembleDebug testDebugUnitTest lintDebug
./gradlew assembleRelease -ParmadioVersionCode=2
```

Release APK: `android-native/app/build/outputs/apk/release/app-release.apk`.
Debug ID is `com.armadio.app.debug`, release is `com.armadio.app`.

## Keys & Updates

Key generated at `signing/armadio-release.jks` (alias: `armadio`).
Passwords in `signing/release.properties`. Both are gitignored.
**Keep a secure backup of both.** Do not regenerate the key for updates, or Android will reject them.

Updates require the same Application ID and key, with an incremented `armadioVersionCode`.
If upgrading from an old Capacitor debug build, uninstall the old app first (this deletes local data) and install the new release.

## GitHub Actions

Set these repository secrets for release builds:
- `ARMADIO_KEYSTORE_BASE64`: base64 encoded keystore
- `ARMADIO_KEYSTORE_PASSWORD`: local `storePassword`
- `ARMADIO_KEY_ALIAS`: `armadio`
- `ARMADIO_KEY_PASSWORD`: local `keyPassword`

Never commit passwords or keystores. The workflow builds, tests, and outputs the signed `armadio-release` artifact. Fails if secrets are missing.
CI versionCode is `GITHUB_RUN_NUMBER + 100`. Local builds must use a higher code than the last release.
