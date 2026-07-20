package net.azisaba.linker

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

@Serializable
data class Config(
    val redisUri: String = "redis://localhost:6379",
    val graphApiKey: String = "",
)

fun loadConfig(path: Path): Config {
    path.createParentDirectories()

    if (!path.exists()) {
        return Config().also { config ->
            path.writeText(json.encodeToString(config))
        }
    }

    return try {
        json.decodeFromString(path.readText())
    } catch (e: Exception) {
        throw IllegalStateException("Failed to load configuration from $path", e)
    }
}
