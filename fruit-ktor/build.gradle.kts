plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

group = "io.github.fruit"
version = "1.0.0"

kotlin {
    android {
        namespace = "io.github.fruit.ktor"
        compileSdk { version = release(36) }
        minSdk { version = release(21) }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":fruit"))
                implementation("io.ktor:ktor-client-core:3.1.1")
                api("io.ktor:ktor-client-content-negotiation:3.1.1")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
            }
        }
    }
}
