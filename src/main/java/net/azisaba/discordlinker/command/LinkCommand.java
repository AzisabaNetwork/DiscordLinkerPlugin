package net.azisaba.discordlinker.command;

import net.azisaba.discordlinker.DiscordLinkerPlugin;
import net.azisaba.discordlinker.database.DatabaseManager;
import net.azisaba.discordlinker.util.LinkCodeGenerator;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LinkCommand implements CommandExecutor {
    
    private final DiscordLinkerPlugin plugin;
    
    public LinkCommand(DiscordLinkerPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "このコマンドはプレイヤーのみが実行できます。");
            return true;
        }
        
        Player player = (Player) sender;
        DatabaseManager databaseManager = plugin.getDatabaseManager();
        
        databaseManager.getPlayerData(player.getUniqueId()).thenAccept(playerData -> {
            if (playerData != null && playerData.isLinked()) {
                player.sendMessage(ChatColor.RED + "あなたのアカウントは既にDiscordにリンクされています。");
                player.sendMessage(ChatColor.RED + "リンクを解除するには、サポートにお問い合わせください。");
                return;
            }
            
            String linkCode = LinkCodeGenerator.generateCode();
            
            databaseManager.createOrUpdatePlayer(player.getUniqueId(), player.getName(), linkCode)
                .thenRun(() -> {
                    player.sendMessage(ChatColor.GREEN + "リンクコードが生成されました！");
                    player.sendMessage(ChatColor.YELLOW + "リンクコード: " + ChatColor.BOLD + linkCode);
                    player.sendMessage(ChatColor.AQUA + "Discordで /link " + linkCode + " を実行してアカウントをリンクしてください。");
                    player.sendMessage(ChatColor.GRAY + "このコードは他の人に教えないでください。");
                })
                .exceptionally(throwable -> {
                    player.sendMessage(ChatColor.RED + "リンクコードの生成中にエラーが発生しました。");
                    plugin.getLogger().severe("Failed to generate link code for player " + player.getName() + ": " + throwable.getMessage());
                    return null;
                });
        }).exceptionally(throwable -> {
            player.sendMessage(ChatColor.RED + "データベースエラーが発生しました。");
            plugin.getLogger().severe("Failed to get player data for " + player.getName() + ": " + throwable.getMessage());
            return null;
        });
        
        return true;
    }
}