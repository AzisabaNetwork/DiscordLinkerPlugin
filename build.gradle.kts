import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin.jvm)
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
    annotationProcessor(libs.velocity.api)
    implementation(libs.graph)
    implementation(libs.mariadb.java.client)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.hikaricp)
}

kotlin {
    jvmToolchain(25)
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
}
