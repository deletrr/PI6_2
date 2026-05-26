plugins {
    kotlin("multiplatform") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    id("com.android.library") version "8.2.0"
}

kotlin {
    androidTarget { compilations.all { kotlinOptions.jvmTarget = "17" } }
    iosX64(); iosArm64(); iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation("io.ktor:ktor-client-core:2.3.11")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.11")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.11")
            implementation("io.ktor:ktor-client-logging:2.3.11")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
        }
        androidMain.dependencies {
            implementation("io.ktor:ktor-client-android:2.3.11")
            implementation("com.google.android.gms:play-services-location:21.3.0")
        }
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:2.3.11")
        }
    }
}

android {
    namespace = "com.parquimetro.shared"
    compileSdk = 34
    defaultConfig.minSdk = 26
}
