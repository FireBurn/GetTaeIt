import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.android.application)

    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

// The google-services plugin hard-fails the build if google-services.json is missing.
// Apply it only once you've dropped the real file in from the Firebase console
// (Project Settings > Your apps > uk.co.fireburn.gettaeit > google-services.json).
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// Read the local.properties file
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

// Release signing comes from keystore.properties, which is never committed.
// Without it, release builds remain unsigned rather than using the debug key.
val releaseKeystore = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { file ->
    Properties().apply { file.inputStream().use(::load) }
}

android {
    namespace = "uk.co.fireburn.gettaeit"
    compileSdk = 37
    ndkVersion = libs.versions.ndk.get()

    defaultConfig {
        applicationId = "uk.co.fireburn.gettaeit"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = rootProject.file(releaseKeystore.getProperty("storeFile"))
                storePassword = releaseKeystore.getProperty("storePassword")
                keyAlias = releaseKeystore.getProperty("keyAlias")
                keyPassword = releaseKeystore.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    implementation(project(":shared"))

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    // Hilt's aggregating javac step needs a kotlin-metadata-jvm that understands
    // our Kotlin version — see the force in the root build.gradle.kts.
    annotationProcessor("org.jetbrains.kotlin:kotlin-metadata-jvm:${libs.versions.kotlinMetadataJvm.get()}")

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    // Data layer and JSON (needed by SettingsViewModel for Export/Delete)
    implementation(libs.androidx.room.runtime)
    implementation(libs.gson)

    // Notifications and WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Permissions
    implementation(libs.accompanist.permissions)

    // Location — needed for FusedLocationProviderClient in SettingsViewModel
    implementation(libs.play.services.location)

    // Sign-in — Credential Manager talks to Google, the raw ID token gets handed
    // to :shared's AuthRepository which does the actual Firebase exchange.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.id)

    // Home screen widgets
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
}
