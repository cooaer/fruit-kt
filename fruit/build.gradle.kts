plugins {
    id("com.android.library")
    kotlin("android")
    id("com.google.devtools.ksp")
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
    implementation("com.fleeksoft.ksoup:ksoup:0.2.6")
    
    // KSP 处理器依赖
    ksp(project(":fruit-ksp"))
    
    // 测试依赖
    testImplementation(kotlin("test"))
}
