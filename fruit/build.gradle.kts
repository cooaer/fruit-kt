plugins {
    kotlin("multiplatform") version "1.9.22"
    id("com.android.library") version "8.2.2"
    id("com.google.devtools.ksp") version "1.9.22-1.0.17"
}

kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.fleeksoft.ksoup:ksoup:0.1.0")
                implementation("io.ktor:ktor-client-core:2.3.7")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
            }
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", project(":fruit-ksp"))
    add("kspAndroid", project(":fruit-ksp"))
    add("kspIosX64", project(":fruit-ksp"))
    add("kspIosArm64", project(":fruit-ksp"))
    add("kspIosSimulatorArm64", project(":fruit-ksp"))
}

android {
    namespace = "io.github.fruit"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
