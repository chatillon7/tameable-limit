package com.chatillon7.tameablelimit;

import com.chatillon7.tameablelimit.storage.IStorage;
import com.chatillon7.tameablelimit.storage.JsonStorage;
import com.chatillon7.tameablelimit.storage.MySQLStorage;
import com.chatillon7.tameablelimit.storage.SQLiteStorage;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TameableLimitPlugin extends JavaPlugin implements Listener {
    private HashMap<UUID, HashMap<String, Integer>> playerTameableCounts;
    private FileConfiguration config;
    private IStorage storage;

    @Override
    public void onEnable() {
        // Config oluşturma
        saveDefaultConfig();
        config = getConfig();

        // Storage sistemini başlat
        initializeStorage();

        // Veri yapısını başlat
        playerTameableCounts = new HashMap<>();

        // Event listener'ı kaydet
        getServer().getPluginManager().registerEvents(this, this);

        // PlaceholderAPI entegrasyonu
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TameablePlaceholders(this).register();
        }

        // Otomatik kaydetme görevi
        int saveInterval = config.getInt("auto-save-interval", 300) * 20; // tick'e çevir
        getServer().getScheduler().runTaskTimerAsynchronously(this, this::saveAllData, saveInterval, saveInterval);

        getLogger().info("TameableLimit plugin has been enabled!");
    }

    private void initializeStorage() {
        String storageType = config.getString("storage.type", "JSON").toUpperCase();

        switch (storageType) {
            case "MYSQL":
                String host = config.getString("storage.mysql.host");
                int port = config.getInt("storage.mysql.port");
                String database = config.getString("storage.mysql.database");
                String username = config.getString("storage.mysql.username");
                String password = config.getString("storage.mysql.password");
                storage = new MySQLStorage(host, port, database, username, password);
                break;

            case "SQLITE":
                storage = new SQLiteStorage(getDataFolder());
                break;

            default:
                storage = new JsonStorage(getDataFolder());
                break;
        }

        storage.initialize();
    }

    @EventHandler
    public void onEntityTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player)) return;
        Player player = (Player) event.getOwner();
        String mobType = event.getEntity().getType().name();

        int limit = getPlayerLimit(player);
        int currentCount = getTameableCount(player.getUniqueId(), mobType);

        if (currentCount >= limit) {
            event.setCancelled(true);
            String message = config.getString("messages.limit-reached", "&cLimit reached!")
                    .replace("{limit}", String.valueOf(limit));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            return;
        }

        // Başarılı evcilleştirme
        incrementTameableCount(player.getUniqueId(), mobType);
        String message = config.getString("messages.successful-tame", "&aSuccessfully tamed!")
                .replace("{current}", String.valueOf(currentCount + 1))
                .replace("{limit}", String.valueOf(limit));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Tameable)) {
            return;
        }

        Tameable tameable = (Tameable) event.getEntity();
        if (!tameable.isTamed() || tameable.getOwner() == null) {
            return;
        }

        UUID ownerUUID = tameable.getOwner().getUniqueId();
        String mobType = event.getEntity().getType().name();

        getLogger().info("Tamed " + mobType + " died. Owner: " + tameable.getOwner().getName());

        decrementTameableCount(ownerUUID, mobType);

        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            Map<String, Integer> data = playerTameableCounts.get(ownerUUID);
            if (data != null) {
                storage.saveTameableData(ownerUUID, data);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Tameable) {
                Tameable tameable = (Tameable) entity;
                if (tameable.isTamed() && tameable.getOwner() != null && entity.isDead()) {
                    UUID ownerUUID = tameable.getOwner().getUniqueId();
                    String mobType = entity.getType().name();

                    getLogger().info("Tamed " + mobType + " died during chunk unload. Owner: " + tameable.getOwner().getName());

                    decrementTameableCount(ownerUUID, mobType);

                    getServer().getScheduler().runTaskAsynchronously(this, () -> {
                        Map<String, Integer> data = playerTameableCounts.get(ownerUUID);
                        if (data != null) {
                            storage.saveTameableData(ownerUUID, data);
                        }
                    });
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            Map<String, Integer> data = storage.loadTameableData(player.getUniqueId());
            playerTameableCounts.put(player.getUniqueId(), new HashMap<>(data));
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            Map<String, Integer> data = playerTameableCounts.remove(player.getUniqueId());
            if (data != null) {
                storage.saveTameableData(player.getUniqueId(), data);
            }
        });
    }

    private void saveAllData() {
        for (Map.Entry<UUID, HashMap<String, Integer>> entry : playerTameableCounts.entrySet()) {
            storage.saveTameableData(entry.getKey(), entry.getValue());
        }
    }

    private int getPlayerLimit(Player player) {
        for (int i = 100; i > 0; i--) {
            if (player.hasPermission("tameablelimit." + i)) {
                return i;
            }
        }
        return config.getInt("settings.default-limit", 1);
    }

    public int getTameableCount(UUID playerUUID, String mobType) {
        return playerTameableCounts
                .computeIfAbsent(playerUUID, k -> new HashMap<>())
                .getOrDefault(mobType, 0);
    }

    private void incrementTameableCount(UUID playerUUID, String mobType) {
        playerTameableCounts
                .computeIfAbsent(playerUUID, k -> new HashMap<>())
                .merge(mobType, 1, Integer::sum);
    }

    private void decrementTameableCount(UUID playerUUID, String mobType) {
        playerTameableCounts.computeIfAbsent(playerUUID, k -> new HashMap<>());
        Map<String, Integer> counts = playerTameableCounts.get(playerUUID);

        int currentCount = counts.getOrDefault(mobType, 0);
        if (currentCount > 0) {
            counts.put(mobType, currentCount - 1);

            if (Bukkit.getPlayer(playerUUID) != null) {
                getLogger().info("Decreased " + mobType + " count for " + Bukkit.getPlayer(playerUUID).getName() +
                        " to " + (currentCount - 1));
            }
        }
    }

    @Override
    public void onDisable() {
        saveAllData();
        storage.close();
        getLogger().info("TameableLimit plugin has been disabled!");
    }
}