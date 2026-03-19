plugins {
    id("com.android.library") version "8.2.2"
    kotlin("android") version "1.9.22"
}

android {
    namespace = "io.github.fruit.converter.retrofit"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
}

dependencies {
    implementation(project(":fruit"))
 // 依赖核心 fruit-kt
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("org.jsoup:jsoup:1.15.4") // Retrofit 端可以保留对 Jsoup 的优化
}

repositories {
    google()
    mavenCentral()
}
