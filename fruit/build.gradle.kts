plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

group = "io.github.fruit"
version = "1.0.0"

kotlin {
    android {
        namespace = "io.github.fruit"
        compileSdk { version = release(36) }
        minSdk { version = release(21) }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api("com.fleeksoft.ksoup:ksoup:0.2.6")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
