package net.azisaba.discordlinker.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.azisaba.discordlinker.DiscordLinkerPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {
    
    private final DiscordLinkerPlugin plugin;
    private HikariDataSource dataSource;
    
    public DatabaseManager(DiscordLinkerPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void initialize() throws SQLException {
        FileConfiguration config = plugin.getConfig();
        
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(String.format("jdbc:mariadb://%s:%d/%s", 
            config.getString("database.host"),
            config.getInt("database.port"),
            config.getString("database.database")));
        hikariConfig.setUsername(config.getString("database.username"));
        hikariConfig.setPassword(config.getString("database.password"));
        hikariConfig.setConnectionTimeout(config.getLong("database.connection-timeout"));
        hikariConfig.setIdleTimeout(config.getLong("database.idle-timeout"));
        hikariConfig.setMaxLifetime(config.getLong("database.max-lifetime"));
        hikariConfig.setMaximumPoolSize(config.getInt("database.maximum-pool-size"));
        
        dataSource = new HikariDataSource(hikariConfig);
        
        createTablesIfNotExists();
    }
    
    private void createTablesIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS players (" +
                     "id VARCHAR(36) PRIMARY KEY, " +
                     "name VARCHAR(32) NOT NULL, " +
                     "discord_id VARCHAR(100), " +
                     "link_code VARCHAR(8)" +
                     ")";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
    
    public CompletableFuture<PlayerData> getPlayerData(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM players WHERE id = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerId.toString());
                ResultSet resultSet = statement.executeQuery();
                
                if (resultSet.next()) {
                    return new PlayerData(
                        UUID.fromString(resultSet.getString("id")),
                        resultSet.getString("name"),
                        resultSet.getString("discord_id"),
                        resultSet.getString("link_code")
                    );
                }
                return null;
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get player data: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    public CompletableFuture<Void> createOrUpdatePlayer(UUID playerId, String playerName, String linkCode) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO players (id, name, discord_id, link_code) VALUES (?, ?, NULL, ?) " +
                        "ON DUPLICATE KEY UPDATE name = ?, link_code = ?";
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerId.toString());
                statement.setString(2, playerName);
                statement.setString(3, linkCode);
                statement.setString(4, playerName);
                statement.setString(5, linkCode);
                
                statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to create/update player: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
    
    public static class PlayerData {
        private final UUID id;
        private final String name;
        private final String discordId;
        private final String linkCode;
        
        public PlayerData(UUID id, String name, String discordId, String linkCode) {
            this.id = id;
            this.name = name;
            this.discordId = discordId;
            this.linkCode = linkCode;
        }
        
        public UUID getId() { return id; }
        public String getName() { return name; }
        public String getDiscordId() { return discordId; }
        public String getLinkCode() { return linkCode; }
        
        public boolean isLinked() {
            return discordId != null && !discordId.isEmpty();
        }
    }
}