plugins {
    id("com.android.library") version "8.2.2"
    kotlin("android") version "1.9.22"
    id("com.google.devtools.ksp") version "1.9.22-1.0.17"
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("com.fleeksoft.ksoup:ksoup:0.1.0")
    
    // KSP 处理器依赖
    ksp(project(":fruit-ksp"))
    
    // 测试依赖
    testImplementation(kotlin("test"))
}

repositories {
    google()
    mavenCentral()
}
