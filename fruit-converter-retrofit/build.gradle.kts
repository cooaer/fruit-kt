plugins {
    id("com.android.library")
}

group = "io.github.fruit"
version = "1.0.0"

android {
    namespace = "io.github.fruit.converter.retrofit"
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
    api(project(":fruit"))
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    testImplementation("com.fleeksoft.ksoup:ksoup:0.2.6")
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}
