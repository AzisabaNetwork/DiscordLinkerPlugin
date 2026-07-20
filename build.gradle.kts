import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
}

group = "net.azisaba"
version = "1.0.0"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://repo.azisaba.net/repository/maven-snapshots/") }
}

dependencies {
    compileOnly(libs.velocity.api)
    kapt(libs.velocity.api)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.mccoroutine.velocity.api)
    implementation(libs.mccoroutine.velocity.core)
    implementation(libs.lettuce.core)
    implementation(libs.graph)
    implementation(libs.nanoid)
}

kotlin {
    jvmToolchain(25)
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
}
