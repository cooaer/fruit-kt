plugins {
    id("com.android.library")
    kotlin("android")
    id("com.google.devtools.ksp")
}

group = "io.github.fruit"
version = "1.0.0"

android {
    namespace = "io.github.fruit.sample.ksp"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":fruit"))
    add("ksp", project(":fruit-ksp"))

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}
