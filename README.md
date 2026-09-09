# Armadio

The native Android app is in `android-native/` (built with Kotlin, Jetpack Compose, and Room).

## Features
- **Wardrobe Management**: Add, search, edit, and delete clothes.
- **Item Details**: Track name, brand, size, and custom notes for each item.
- **Privacy First**: No internet permissions required, everything is stored locally on the device.
- **Planned Features**: Photos, outfits, trips, and export/import functionality.

## Architecture
- **UI**: Jetpack Compose (100% declarative UI).
- **Database**: Room (SQLite abstraction for local storage).
- **Language**: Kotlin.
- **Legacy Web Code**: `src/` contains only a presentation webpage and is not packaged into the APK. Do not build with Capacitor.

The `.github/workflows/android-build.yml` workflow is set up for native app compilation.

## Local Build

Requirements:
- JDK 17
- Android SDK 35
- Android Build Tools 34.0.0

Set `sdk.dir` in `android-native/local.properties` (e.g., `sdk.dir=/Users/username/Library/Android/sdk`).

```bash
bash scripts/create-signing-key.sh # First time only, to generate signing keys
cd android-native
./gradlew assembleDebug testDebugUnitTest lintDebug
./gradlew assembleRelease -ParmadioVersionCode=2
```

Release APK path: `android-native/app/build/outputs/apk/release/app-release.apk`.
- Debug Application ID: `com.armadio.app.debug`
- Release Application ID: `com.armadio.app`

## Keys & Updates

A release key is generated at `signing/armadio-release.jks` with alias `armadio`.
Passwords are saved in `signing/release.properties`. Both files are excluded from Git to prevent leaking secrets.

**⚠️ Important:** Keep a secure, off-site backup of the keystore and properties file. Do not regenerate the key for updates, or Android will reject the APK as it won't match the original signature.

Updates require the same Application ID, the same signing key, and a strictly incremented `armadioVersionCode`.
If upgrading from an old Capacitor debug build, uninstall the old app first (this deletes local data) and install the new release.

## CI/CD (GitHub Actions)

Set these repository secrets in GitHub for release builds:
- `ARMADIO_KEYSTORE_BASE64`: base64 encoded keystore file content.
- `ARMADIO_KEYSTORE_PASSWORD`: Local `storePassword`.
- `ARMADIO_KEY_ALIAS`: `armadio`
- `ARMADIO_KEY_PASSWORD`: Local `keyPassword`.

Never commit passwords or keystores. The workflow builds, tests, and outputs the signed `armadio-release` artifact. It fails if secrets are missing instead of generating a temporary key.
The CI `versionCode` is computed as `GITHUB_RUN_NUMBER + 100`. Local builds must manually use a higher `versionCode` than the last distributed release.

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
