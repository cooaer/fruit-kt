plugins {
    id("com.android.library")
    id("com.google.devtools.ksp")
}

group = "io.github.fruit"
version = "1.0.0"

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

dependencies {
    implementation("com.fleeksoft.ksoup:ksoup:0.2.6")
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    
    // KSP 处理器依赖
    ksp(project(":fruit-ksp"))
    
    // 测试依赖
    testImplementation(kotlin("test"))
}
