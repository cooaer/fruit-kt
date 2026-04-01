plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
}

group = "io.github.fruit"
version = "1.0.0"

kotlin {
    android {
        namespace = "io.github.fruit"
        compileSdk { version = release(36) }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "fruit"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.fleeksoft.ksoup:ksoup:0.2.6")
                implementation("io.ktor:ktor-client-core:3.1.1")
                implementation("io.ktor:ktor-client-content-negotiation:3.1.1")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting

        val iosMain by creating {
            dependsOn(commonMain)
        }
    }
}

dependencies {
    // KSP 处理器依赖
    add("kspAndroid", project(":fruit-ksp"))
}
