plugins {
    kotlin("jvm") version "1.9.22"
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.22-1.0.17")
    implementation("com.squareup:kotlinpoet:1.15.3") // 用于生成 Kotlin 代码的利器
    implementation("com.squareup:kotlinpoet-ksp:1.15.3")
}

repositories {
    mavenCentral()
    google()
}
