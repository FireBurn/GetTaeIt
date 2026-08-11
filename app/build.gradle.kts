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

android {
    namespace = "uk.co.fireburn.gettaeit"
    compileSdk = 37

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

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Permissions
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.work.runtime.ktx)

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
