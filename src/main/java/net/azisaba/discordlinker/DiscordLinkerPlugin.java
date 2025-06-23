package net.azisaba.discordlinker;

import net.azisaba.discordlinker.command.LinkCommand;
import net.azisaba.discordlinker.database.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

public class DiscordLinkerPlugin extends JavaPlugin {
    
    private DatabaseManager databaseManager;
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        try {
            databaseManager = new DatabaseManager(this);
            databaseManager.initialize();
            getLogger().info("Database connection established successfully");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        getCommand("link-azisaba-discord").setExecutor(new LinkCommand(this));
        
        getLogger().info("DiscordLinkerPlugin has been enabled");
    }
    
    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("DiscordLinkerPlugin has been disabled");
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}