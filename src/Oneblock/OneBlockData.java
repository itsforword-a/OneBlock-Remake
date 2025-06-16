package Oneblock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OneBlockData {
    private final Location location;
    private int stage;
    private int progress;
    private final List<BlockStage> stages;
    private static final Random random = new Random();

    public OneBlockData(Location location) {
        this.location = location;
        this.stage = 0;
        this.progress = 0;
        this.stages = loadStages();
    }

    public OneBlockData(Location location, ConfigurationSection section) {
        this.location = location;
        this.stage = section.getInt("stage", 0);
        this.progress = section.getInt("progress", 0);
        this.stages = loadStages();
    }

    public void breakBlock(Player player) {
        Block block = location.getBlock();
        String mode = OneBlockPlugin.getInstance().getConfig().getString("mode", "scenario");
        
        if (mode.equals("scenario")) {
            handleScenarioMode(block, player);
        } else {
            handleRandomMode(block, player);
        }
        
        progress++;
    }

    private void handleScenarioMode(Block block, Player player) {
        BlockStage currentStage = stages.get(stage);
        if (progress >= currentStage.getBlocksRequired()) {
            stage++;
            progress = 0;
            if (stage >= stages.size()) {
                stage = 0;
            }
        }
        
        BlockStage nextStage = stages.get(stage);
        block.setType(nextStage.getNextBlock());
        
        if (nextStage.shouldSpawnMob() && random.nextDouble() < nextStage.getMobSpawnChance()) {
            spawnMob(nextStage.getRandomMob());
        }
    }

    private void handleRandomMode(Block block, Player player) {
        BlockStage randomStage = stages.get(random.nextInt(stages.size()));
        block.setType(randomStage.getNextBlock());
        
        if (randomStage.shouldSpawnMob() && random.nextDouble() < randomStage.getMobSpawnChance()) {
            spawnMob(randomStage.getRandomMob());
        }
    }

    private void spawnMob(EntityType type) {
        if (type != null) {
            location.getWorld().spawnEntity(location.clone().add(0.5, 1, 0.5), type);
        }
    }

    private List<BlockStage> loadStages() {
        List<BlockStage> stages = new ArrayList<>();
        ConfigurationSection stagesSection = OneBlockPlugin.getInstance().getConfig().getConfigurationSection("stages");
        
        if (stagesSection != null) {
            for (String key : stagesSection.getKeys(false)) {
                ConfigurationSection stageSection = stagesSection.getConfigurationSection(key);
                if (stageSection != null) {
                    stages.add(new BlockStage(stageSection));
                }
            }
        }
        
        return stages;
    }

    public void save(ConfigurationSection section) {
        section.set("stage", stage);
        section.set("progress", progress);
    }

    public String getCurrentStage() {
        return stages.get(stage).getName();
    }

    public double getProgress() {
        BlockStage currentStage = stages.get(stage);
        return (double) progress / currentStage.getBlocksRequired();
    }

    private static class BlockStage {
        private final String name;
        private final List<Material> blocks;
        private final int blocksRequired;
        private final List<EntityType> mobs;
        private final double mobSpawnChance;

        public BlockStage(ConfigurationSection section) {
            this.name = section.getString("name", "Unknown");
            this.blocks = new ArrayList<>();
            this.mobs = new ArrayList<>();
            
            for (String block : section.getStringList("blocks")) {
                try {
                    blocks.add(Material.valueOf(block.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    // Invalid material, skip
                }
            }
            
            for (String mob : section.getStringList("mobs")) {
                try {
                    mobs.add(EntityType.valueOf(mob.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    // Invalid entity type, skip
                }
            }
            
            this.blocksRequired = section.getInt("blocks-required", 100);
            this.mobSpawnChance = section.getDouble("mob-spawn-chance", 0.1);
        }

        public String getName() {
            return name;
        }

        public Material getNextBlock() {
            return blocks.get(random.nextInt(blocks.size()));
        }

        public int getBlocksRequired() {
            return blocksRequired;
        }

        public boolean shouldSpawnMob() {
            return !mobs.isEmpty();
        }

        public double getMobSpawnChance() {
            return mobSpawnChance;
        }

        public EntityType getRandomMob() {
            return mobs.get(random.nextInt(mobs.size()));
        }
    }
} 