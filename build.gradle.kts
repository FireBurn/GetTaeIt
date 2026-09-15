// Root build.gradle.kts
plugins {
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    // Applied conditionally in :app, once google-services.json exists — see there.
    alias(libs.plugins.google.services) apply false
}

// Dagger/Hilt 2.60.1 bundles a kotlin-metadata-jvm (2.3.21) that can't parse Kotlin 2.4
// metadata. It's unshaded since Dagger 2.57, so force the matching version here
// rather than pin the whole project back to an older Kotlin.
subprojects {
    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-metadata-jvm:${libs.versions.kotlinMetadataJvm.get()}")
        }
    }
}

// Clean task
tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
