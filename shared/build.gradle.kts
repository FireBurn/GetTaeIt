plugins {
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "uk.co.fireburn.gettaeit.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
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
    // Room (offline-first database)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // JSON (for Room TypeConverters)
    implementation(libs.gson)

    // WorkManager (background recurrence reset)
    implementation(libs.androidx.work.runtime.ktx)

    // Location / Geofencing (no Maps UI — just the location services client)
    implementation(libs.play.services.location)

    // Wearable Data Layer (phone <-> watch sync, no Firebase needed)
    implementation(libs.play.services.wearable)

    // DataStore (user preferences)
    implementation(libs.androidx.datastore.preferences)
}
