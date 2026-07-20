package net.azisaba.linker.commands

import com.github.shynixn.mccoroutine.velocity.executesSuspend
import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import io.lettuce.core.SetArgs
import io.lettuce.core.api.async.RedisAsyncCommands
import io.viascom.nanoid.NanoId
import kotlinx.coroutines.future.await
import net.azisaba.graph.ApiException
import net.azisaba.graph.api.PlayersApi
import net.azisaba.linker.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.slf4j.Logger
import java.time.Duration

private const val LINK_CODE_LENGTH: Int = 8
private const val LINK_CODE_ALPHABET: String = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
private val LINK_CODE_TTL: Duration = Duration.ofMinutes(5)

private const val REDIS_KEY_PREFIX: String = "discord-link:"

private const val MAX_LINK_CODE_GENERATION_ATTEMPTS: Int = 10

object LinkDiscordCommand {
    fun createBrigadierCommand(
        plugin: Main,
        logger: Logger,
        playersApi: PlayersApi,
        redisConnection: RedisAsyncCommands<String, String>,
    ): BrigadierCommand {
        val node = BrigadierCommand.literalArgumentBuilder("link-discord")
            .requires { source -> source is Player }
            .executesSuspend(plugin) { context ->
                execute(context, logger, playersApi, redisConnection)
            }
            .build()

        return BrigadierCommand(node)
    }

    private suspend fun execute(
        context: CommandContext<CommandSource>,
        logger: Logger,
        playersApi: PlayersApi,
        redisConnection: RedisAsyncCommands<String, String>,
    ): Int {
        val player = context.source as Player

        if (playersApi.isDiscordAccountLinked(player)) {
            player.sendMessage(Component.translatable("command.link-discord.already-linked", NamedTextColor.RED))
            return 0
        }

        val linkCode = try {
            redisConnection.issueLinkCode(player)
        } catch (exception: Exception) {
            player.sendMessage(Component.translatable("command.link-discord.failed", NamedTextColor.RED))
            logger.error("Failed to issue a link code for {}", player.username, exception)
            return 0
        }

        player.sendMessage(
            Component.text()
                .appendNewline()
                .append(Component.translatable("command.link-discord.issued", NamedTextColor.GREEN))
                .appendNewline()
                .append(
                    Component.text()
                        .content("/link-minecraft $linkCode")
                        .color(NamedTextColor.GRAY)
                        .hoverEvent(Component.translatable("chat.copy"))
                        .clickEvent(ClickEvent.copyToClipboard("/link-minecraft code:$linkCode"))
                        .build()
                )
                .appendNewline()
                .append(
                    Component.translatable()
                        .key("command.link-discord.join")
                        .color(TextColor.color(0x5865f2))
                        .decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl("https://discord.gg/azisaba"))
                        .build()
                )
                .appendNewline()
                .build()
        )

        return Command.SINGLE_SUCCESS
    }

    private suspend fun PlayersApi.isDiscordAccountLinked(player: Player): Boolean {
        return try {
            getPlayerById(player.uniqueId).await().discordId != null
        } catch (exception: ApiException) {
            if (exception.code == 404) false else throw exception
        }
    }

    private suspend fun RedisAsyncCommands<String, String>.issueLinkCode(player: Player): String {
        repeat(MAX_LINK_CODE_GENERATION_ATTEMPTS) {
            val linkCode = NanoId.generate(size = LINK_CODE_LENGTH, alphabet = LINK_CODE_ALPHABET)
            val result = set(
                "$REDIS_KEY_PREFIX$linkCode",
                player.uniqueId.toString(),
                SetArgs.Builder.nx().ex(LINK_CODE_TTL),
            ).await()

            if (result == "OK") {
                return linkCode
            }
        }

        error("Could not allocate a unique link code after $MAX_LINK_CODE_GENERATION_ATTEMPTS attempts")
    }
}
