plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "net.azisaba"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
    implementation("org.mariadb.jdbc:mariadb-java-client:2.7.9")
    implementation("com.zaxxer:HikariCP:4.0.3")
    
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveClassifier.set("")
    
    relocate("org.mariadb", "net.azisaba.discordlinker.libs.mariadb")
    relocate("com.zaxxer.hikari", "net.azisaba.discordlinker.libs.hikari")
    relocate("org.slf4j", "net.azisaba.discordlinker.libs.slf4j")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}