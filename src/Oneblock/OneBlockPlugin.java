package Oneblock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class OneBlockPlugin extends JavaPlugin implements Listener {
    private static OneBlockPlugin instance;
    private FileConfiguration config;
    private FileConfiguration messages;
    private Map<Location, OneBlockData> oneBlocks;
    private Map<UUID, BossBar> playerBossBars;
    private int radius;
    private String mode;
    private boolean bossBarEnabled;

    @Override
    public void onEnable() {
        instance = this;
        
        // Load configurations
        saveDefaultConfig();
        config = getConfig();
        loadMessages();
        
        // Initialize data structures
        oneBlocks = new HashMap<>();
        playerBossBars = new HashMap<>();
        
        // Load settings
        radius = config.getInt("radius", 10);
        mode = config.getString("mode", "scenario");
        bossBarEnabled = config.getBoolean("bossbar.enabled", true);
        
        // Register events
        getServer().getPluginManager().registerEvents(this, this);
        
        // Load saved OneBlocks
        loadOneBlocks();
        
        // Start bossbar update task
        if (bossBarEnabled) {
            startBossBarUpdateTask();
        }
    }

    @Override
    public void onDisable() {
        saveOneBlocks();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("setoneblock")) {
            if (!player.isOp()) {
                player.sendMessage(getMessage("no-permission"));
                return true;
            }
            
            Location loc = player.getLocation().getBlock().getLocation();
            oneBlocks.put(loc, new OneBlockData(loc));
            saveOneBlocks();
            player.sendMessage(getMessage("oneblock-set"));
            return true;
        }

        if (command.getName().equalsIgnoreCase("removeoneblock")) {
            Location loc = player.getLocation().getBlock().getLocation();
            if (oneBlocks.remove(loc) != null) {
                saveOneBlocks();
                player.sendMessage(getMessage("oneblock-removed"));
            } else {
                player.sendMessage(getMessage("no-oneblock-here"));
            }
            return true;
        }

        return false;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        OneBlockData data = oneBlocks.get(loc);
        
        if (data != null) {
            event.setCancelled(true);
            data.breakBlock(event.getPlayer());
            updateNearbyBossBars(loc);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (bossBarEnabled) {
            updatePlayerBossBar(event.getPlayer());
        }
    }

    private void updateNearbyBossBars(Location loc) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getLocation().distance(loc) <= radius) {
                updatePlayerBossBar(player);
            }
        }
    }

    private void updatePlayerBossBar(Player player) {
        BossBar bossBar = playerBossBars.computeIfAbsent(player.getUniqueId(), 
            uuid -> Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID));
            
        OneBlockData nearest = findNearestOneBlock(player.getLocation());
        if (nearest != null) {
            bossBar.setTitle(getBossBarTitle(player, nearest));
            bossBar.setProgress(nearest.getProgress());
            bossBar.addPlayer(player);
        } else {
            bossBar.removePlayer(player);
        }
    }

    private OneBlockData findNearestOneBlock(Location loc) {
        OneBlockData nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Map.Entry<Location, OneBlockData> entry : oneBlocks.entrySet()) {
            double distance = loc.distance(entry.getKey());
            if (distance <= radius && distance < minDistance) {
                minDistance = distance;
                nearest = entry.getValue();
            }
        }
        
        return nearest;
    }

    private String getBossBarTitle(Player player, OneBlockData data) {
        String title = getMessage("bossbar-format")
            .replace("%stage%", data.getCurrentStage())
            .replace("%progress%", String.valueOf(data.getProgress()));
            
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            title = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, title);
        }
        
        return title;
    }

    private void startBossBarUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updatePlayerBossBar(player);
                }
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    private void loadMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private String getMessage(String key) {
        return messages.getString(key, "Message not found: " + key);
    }

    private void loadOneBlocks() {
        File dataFile = new File(getDataFolder(), "oneblocks.yml");
        if (dataFile.exists()) {
            FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
            for (String key : data.getKeys(false)) {
                String[] coords = key.split(",");
                Location loc = new Location(
                    Bukkit.getWorld(coords[0]),
                    Integer.parseInt(coords[1]),
                    Integer.parseInt(coords[2]),
                    Integer.parseInt(coords[3])
                );
                oneBlocks.put(loc, new OneBlockData(loc, data.getConfigurationSection(key)));
            }
        }
    }

    private void saveOneBlocks() {
        File dataFile = new File(getDataFolder(), "oneblocks.yml");
        FileConfiguration data = new YamlConfiguration();
        
        for (Map.Entry<Location, OneBlockData> entry : oneBlocks.entrySet()) {
            Location loc = entry.getKey();
            String key = String.format("%s,%d,%d,%d", 
                loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            entry.getValue().save(data.createSection(key));
        }
        
        try {
            data.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("Could not save oneblocks.yml: " + e.getMessage());
        }
    }

    public static OneBlockPlugin getInstance() {
        return instance;
    }
} 