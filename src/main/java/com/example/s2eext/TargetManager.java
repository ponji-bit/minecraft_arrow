package com.example.s2eext;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class TargetManager {

    private static final String WORLD_NAME = "world";
    private static final int BASE_X = 100;
    private static final int BASE_Y = 65;
    private static final int BASE_Z = 100;

    private static final int GRID_WIDTH = 20;
    private static final int GRID_LENGTH = 20;
    private static final int MAX_LAYERS = 8;

    private static final int GENERATE_PER_TICK = 12;
    private static final long INITIAL_CHECK_DELAY_TICKS = 40L;
    private static final long REGEN_DELAY_AFTER_DESTROY_TICKS = 20L;

    private final JavaPlugin plugin;

    private int currentTargetCount;
    private int targetAmount = 300;

    private boolean regenerateQueued;
    private BukkitTask generateTask;

    public TargetManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void checkInitialTargets() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int existing = countTargetsInManagedArea();
            currentTargetCount = existing;

            if (existing < targetAmount) {
                plugin.getLogger().info("Initial targets detected: " + existing + ". Filling to " + targetAmount + ".");
                generateMissingTargets(targetAmount);
            } else {
                plugin.getLogger().info("Initial targets detected: " + existing + ". No fill required.");
            }
        }, INITIAL_CHECK_DELAY_TICKS);
    }

    public void handleTargetBlockBreak(Block block) {
        if (!isTargetBlock(block)) {
            return;
        }

        if (currentTargetCount > 0) {
            currentTargetCount--;
        }

        if (currentTargetCount <= 0) {
            queueRegenerationAfterDestroyed();
        }
    }

    public boolean isTargetBlock(Block block) {
        if (!block.getWorld().getName().equals(WORLD_NAME)) {
            return false;
        }
        if (block.getY() < BASE_Y || block.getY() >= BASE_Y + MAX_LAYERS) {
            return false;
        }
        if (block.getX() < BASE_X || block.getX() >= BASE_X + GRID_WIDTH) {
            return false;
        }
        if (block.getZ() < BASE_Z || block.getZ() >= BASE_Z + GRID_LENGTH) {
            return false;
        }

        Material type = block.getType();
        return type == Material.RED_CONCRETE || type == Material.WHITE_CONCRETE;
    }

    public int countTargetsInManagedArea() {
        World world = Bukkit.getWorld(WORLD_NAME);
        if (world == null) {
            return 0;
        }

        int count = 0;
        for (int layer = 0; layer < MAX_LAYERS; layer++) {
            int y = BASE_Y + layer;
            for (int x = BASE_X; x < BASE_X + GRID_WIDTH; x++) {
                for (int z = BASE_Z; z < BASE_Z + GRID_LENGTH; z++) {
                    Material type = world.getBlockAt(x, y, z).getType();
                    if (type == Material.RED_CONCRETE || type == Material.WHITE_CONCRETE) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public void regenerateToConfiguredAmount() {
        queueDirectRegeneration(targetAmount);
    }

    private void queueRegenerationAfterDestroyed() {
        if (regenerateQueued) {
            return;
        }

        regenerateQueued = true;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            regenerateQueued = false;
            queueDirectRegeneration(targetAmount);
        }, REGEN_DELAY_AFTER_DESTROY_TICKS);
    }

    private void queueDirectRegeneration(int amount) {
        stopGeneration();
        generateMissingTargets(amount);
    }

    private void generateMissingTargets(int desiredAmount) {
        int boundedAmount = Math.min(desiredAmount, getMaxTargetSlots());
        if (desiredAmount > boundedAmount) {
            plugin.getLogger().warning("Requested target amount " + desiredAmount + " exceeds max slots " + boundedAmount + ". Clamped.");
        }

        World world = Bukkit.getWorld(WORLD_NAME);
        if (world == null) {
            plugin.getLogger().warning("World not found: " + WORLD_NAME);
            return;
        }

        generateTask = new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                int placedThisTick = 0;

                while (index < boundedAmount && placedThisTick < GENERATE_PER_TICK) {
                    Location location = slotToLocation(world, index);
                    Material material = (index % 2 == 0) ? Material.RED_CONCRETE : Material.WHITE_CONCRETE;

                    Block block = world.getBlockAt(location);
                    if (block.getType() != material) {
                        block.setType(material, false);
                    }

                    index++;
                    placedThisTick++;
                }

                if (index >= boundedAmount) {
                    currentTargetCount = boundedAmount;
                    generateTask = null;
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private Location slotToLocation(World world, int index) {
        int perLayer = GRID_WIDTH * GRID_LENGTH;
        int layer = index / perLayer;
        int inLayer = index % perLayer;

        int xOffset = inLayer % GRID_WIDTH;
        int zOffset = inLayer / GRID_WIDTH;

        return new Location(world, BASE_X + xOffset, BASE_Y + layer, BASE_Z + zOffset);
    }

    public void stopGeneration() {
        if (generateTask != null) {
            generateTask.cancel();
            generateTask = null;
        }
    }

    public int getCurrentTargetCount() {
        return currentTargetCount;
    }

    public int getTargetAmount() {
        return targetAmount;
    }

    public boolean updateTargetAmount(int newAmount) {
        if (newAmount <= 0 || newAmount > getMaxTargetSlots()) {
            return false;
        }
        this.targetAmount = newAmount;
        return true;
    }

    public int getMaxTargetSlots() {
        return GRID_WIDTH * GRID_LENGTH * MAX_LAYERS;
    }
}
