package net.azisaba.linker

import com.github.shynixn.mccoroutine.velocity.SuspendingPluginContainer
import com.google.inject.Inject
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.*
import net.azisaba.graph.ApiClient
import net.azisaba.graph.api.PlayersApi
import net.azisaba.linker.commands.LinkDiscordCommand
import net.azisaba.linker.commands.UnlinkDiscordCommand
import net.kyori.adventure.key.Key
import net.kyori.adventure.translation.GlobalTranslator
import net.kyori.adventure.translation.TranslationStore
import org.slf4j.Logger
import java.nio.file.Path
import java.text.MessageFormat
import java.util.Locale
import java.util.ResourceBundle

@Plugin(id = "azisaba-discord-linker")
class Main @Inject constructor(
    private val server: ProxyServer,
    private val logger: Logger,
    @DataDirectory private val dataDirectory: Path,
    suspendingPluginContainer: SuspendingPluginContainer,
) {
    private val config: Config = loadConfig(dataDirectory.resolve("config.json"))

    private val coroutineScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineName(DiscordLinker.NAMESPACE))

    private val translationStore: TranslationStore.StringBased<MessageFormat> =
        TranslationStore.messageFormat(Key.key(DiscordLinker.NAMESPACE, "translations"))

    private val redisClient: RedisClient
    private val redisConnection: StatefulRedisConnection<String, String>

    private val playersApi: PlayersApi

    init {
        suspendingPluginContainer.initialize(this)

        redisClient = RedisClient.create(config.redisUri)
        redisConnection = redisClient.connect()

        playersApi = PlayersApi(
            ApiClient().setRequestInterceptor { request ->
                request.header("Authorization", "Bearer ${config.graphApiKey}")
            }
        )
    }

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        registerTranslations(Locale.US)
        registerTranslations(Locale.JAPAN)
        GlobalTranslator.translator().addSource(translationStore)

        registerCommand(LinkDiscordCommand.createBrigadierCommand(this, logger, playersApi, redisConnection.async()))
        registerCommand(UnlinkDiscordCommand.createBrigadierCommand(this, playersApi))
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        GlobalTranslator.translator().removeSource(translationStore)

        coroutineScope.cancel()

        redisConnection.close()
        redisClient.shutdown()
    }

    private fun registerTranslations(locale: Locale) {
        translationStore.registerAll(locale, ResourceBundle.getBundle("translations/Bundle", locale), true)
    }

    private fun registerCommand(command: BrigadierCommand) {
        val commandManager = server.commandManager
        val commandMeta = commandManager.metaBuilder(command)
            .plugin(this)
            .build()
        commandManager.register(commandMeta, command)
    }
}
