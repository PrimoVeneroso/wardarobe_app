import java.util.Properties

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
        applicationId = "com.armadio.app"
        minSdk = 26
        targetSdk = 35
        versionCode = providers.gradleProperty("armadioVersionCode").orElse("2").get().toInt()
        versionName = "0.2.0"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            val signingFile = rootProject.file("../signing/release.properties")
            val signing = Properties()
            if (signingFile.exists()) signingFile.inputStream().use { signing.load(it) }
            val keystore = System.getenv("ARMADIO_KEYSTORE_PATH") ?: signing.getProperty("storeFile")
            if (keystore != null) {
                storeFile = rootProject.file(keystore)
                storePassword = System.getenv("ARMADIO_KEYSTORE_PASSWORD") ?: signing.getProperty("storePassword")
                keyAlias = System.getenv("ARMADIO_KEY_ALIAS") ?: signing.getProperty("keyAlias")
                keyPassword = System.getenv("ARMADIO_KEY_PASSWORD") ?: signing.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug { applicationIdSuffix = ".debug" }
        release {
            signingConfig = signingConfigs.getByName("release")
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
        // Robolectric reads assets from the tested Android variant.
        getByName("debug") {
            assets.srcDir("$projectDir/schemas")
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true // Robolectric
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/AL2.0"
            excludes += "/META-INF/LGPL2.1"
        }
    }
}

tasks.matching { it.name == "validateSigningRelease" }.configureEach {
    doFirst {
        check(android.signingConfigs.getByName("release").storeFile?.isFile == true) {
            "Release key missing. Run scripts/create-signing-key.sh or configure ARMADIO_KEYSTORE_PATH."
        }
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
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
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
        // AGP 8 ("merged_manifest/<variant>/process*MainManifest/merged.xml",
        // "packaged_manifests/<variant>/AndroidManifest.xml", etc.).
        val mergedManifests = intermediates.get().asFile
            .walkTopDown()
            .filter { file ->
                file.isFile &&
                    file.extension.equals("xml", ignoreCase = true) &&
                    (file.absolutePath.contains("merged_manifest", ignoreCase = true) ||
                        file.absolutePath.contains("packaged_manifest", ignoreCase = true))
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
