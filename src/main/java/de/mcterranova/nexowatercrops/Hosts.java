package de.mcterranova.nexowatercrops;

import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.MangrovePropagule;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The host block under a water crop: a standing mangrove propagule with an age below 4, waterlogged. Vanilla always
 * places a standing propagule with age 4 (MangrovePropaguleBlock#getStateForPlacement) and trees only grow hanging
 * ones, so these states are free; the pack gives them an empty model. The client then draws the water of the block
 * and the propagule's outline, and nothing collides.
 * <p>
 * Every host is noted in its chunk, so a host whose crop is gone turns back to water when the chunk loads, even after
 * a crash or an unload in the middle of a stage swap.
 */
final class Hosts {

    private static final NamespacedKey HOSTS = new NamespacedKey("nexowatercrops", "hosts");

    private Hosts() {
    }

    static boolean isHost(@Nullable Block block) {
        return block != null && block.getType() == Material.MANGROVE_PROPAGULE
                && block.getBlockData() instanceof MangrovePropagule propagule
                && !propagule.isHanging() && propagule.getAge() < 4;
    }

    private static BlockData hostData() {
        MangrovePropagule host = (MangrovePropagule) Material.MANGROVE_PROPAGULE.createBlockData();
        host.setHanging(false);
        host.setAge(0);
        // at stage 0 a random tick turns it to 1 without an event; at 1 it can only try to grow a tree, which fires
        // StructureGrowEvent and is cancelled
        host.setStage(1);
        host.setWaterlogged(true);
        return host;
    }

    @Nullable
    static WaterCropMechanic mechanic(Entity entity) {
        WaterCropFactory factory = WaterCropFactory.instance();
        if (factory == null || !(entity instanceof ItemDisplay)) return null;
        FurnitureMechanic furniture = NexoFurniture.furnitureMechanic(entity);
        return furniture == null ? null : factory.getMechanic(furniture.getItemID());
    }

    /** The water crop standing in this block, if any. */
    @Nullable
    static ItemDisplay crop(Block block) {
        for (ItemDisplay display : block.getWorld().getNearbyEntitiesByType(ItemDisplay.class, block.getLocation().toCenterLocation(), 1)) {
            if (display.isValid() && display.getLocation().getBlock().equals(block) && mechanic(display) != null) return display;
        }
        return null;
    }

    /** Puts a host under the crop in this block, if it stands in a water source on ground the propagule holds on. */
    static void ensure(Block block) {
        if (!isLoaded(block) || crop(block) == null) return;
        if (!isHost(block)) {
            if (!isWaterSource(block)) return;
            BlockData host = hostData();
            if (!host.isSupported(block)) return;
            block.setBlockData(host, false);
        }
        note(block, true);
    }

    /** Turns this block back to water if it is a host without a crop. */
    static void release(Block block) {
        if (!isLoaded(block)) return; // its chunk checks again when it loads
        if (!isHost(block)) {
            note(block, false);
            return;
        }
        if (crop(block) == null) toWater(block);
    }

    static void toWater(Block block) {
        block.setType(Material.WATER);
        note(block, false);
    }

    /** Drops the crop's loot like a broken block without a player, then removes the crop. */
    static void dropAndRemove(ItemDisplay crop) {
        FurnitureMechanic furniture = NexoFurniture.furnitureMechanic(crop);
        if (furniture != null) furniture.getBreakable().getDrop().furnitureSpawns(crop, ItemStack.empty());
        NexoFurniture.remove(crop); // without a player Nexo drops nothing itself
    }

    static List<Block> noted(Chunk chunk) {
        int[] xyz = chunk.getPersistentDataContainer().getOrDefault(HOSTS, PersistentDataType.INTEGER_ARRAY, new int[0]);
        World world = chunk.getWorld();
        List<Block> blocks = new ArrayList<>(xyz.length / 3);
        for (int i = 0; i + 2 < xyz.length; i += 3) blocks.add(world.getBlockAt(xyz[i], xyz[i + 1], xyz[i + 2]));
        return blocks;
    }

    private static void note(Block block, boolean host) {
        PersistentDataContainer data = block.getChunk().getPersistentDataContainer();
        int[] xyz = data.getOrDefault(HOSTS, PersistentDataType.INTEGER_ARRAY, new int[0]);
        int at = indexOf(xyz, block);
        if (host == (at >= 0)) return;
        int[] changed;
        if (host) {
            changed = Arrays.copyOf(xyz, xyz.length + 3);
            changed[xyz.length] = block.getX();
            changed[xyz.length + 1] = block.getY();
            changed[xyz.length + 2] = block.getZ();
        } else {
            changed = new int[xyz.length - 3];
            System.arraycopy(xyz, 0, changed, 0, at);
            System.arraycopy(xyz, at + 3, changed, at, xyz.length - at - 3);
        }
        if (changed.length == 0) data.remove(HOSTS);
        else data.set(HOSTS, PersistentDataType.INTEGER_ARRAY, changed);
    }

    private static int indexOf(int[] xyz, Block block) {
        for (int i = 0; i + 2 < xyz.length; i += 3) {
            if (xyz[i] == block.getX() && xyz[i + 1] == block.getY() && xyz[i + 2] == block.getZ()) return i;
        }
        return -1;
    }

    private static boolean isWaterSource(Block block) {
        return block.getType() == Material.WATER && block.getBlockData() instanceof Levelled water && water.getLevel() == 0;
    }

    private static boolean isLoaded(Block block) {
        return block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4);
    }
}
