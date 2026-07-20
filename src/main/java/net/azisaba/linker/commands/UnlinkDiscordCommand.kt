package net.azisaba.linker.commands

import com.github.shynixn.mccoroutine.velocity.executesSuspend
import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import kotlinx.coroutines.future.await
import net.azisaba.graph.api.PlayersApi
import net.azisaba.graph.model.UpdatePlayerByIdRequest
import net.azisaba.linker.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

object UnlinkDiscordCommand {
    fun createBrigadierCommand(
        plugin: Main,
        playersApi: PlayersApi,
    ): BrigadierCommand {
        val node = BrigadierCommand.literalArgumentBuilder("unlink-discord")
            .requires { source -> source is Player }
            .executesSuspend(plugin) { context ->
                execute(context, playersApi)
            }
            .build()

        return BrigadierCommand(node)
    }

    private suspend fun execute(context: CommandContext<CommandSource>, playersApi: PlayersApi): Int {
        val player = context.source as Player

        val apiPlayer = playersApi.getPlayerById(player.uniqueId).await()
        if (apiPlayer.discordId == null) {
            player.sendMessage(Component.translatable("command.unlink-discord.not-linked", NamedTextColor.RED))
            return 0
        }

        playersApi.updatePlayerById(player.uniqueId, UpdatePlayerByIdRequest().discordId(null))
        player.sendMessage(Component.translatable("command.unlink-discord.success", NamedTextColor.GREEN))
        return Command.SINGLE_SUCCESS
    }
}
