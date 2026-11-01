package de.mcterranova.nexowatercrops;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Opibus rolls silver for broken kelp unless the block counts as placed by a player, also when the break gets
 * cancelled. It keeps that in its own CustomBlockData: in the chunk's data under {@code opibus:x<x>y<y>z<z>} a container
 * with {@code wasPlaced}. Written by hand here, Opibus has the library only relocated into its own jar.
 */
final class OpibusMark {

    private OpibusMark() {
    }

    static void set(Block block) {
        Plugin opibus = Bukkit.getPluginManager().getPlugin("Opibus");
        if (opibus == null) return;
        PersistentDataContainer chunk = block.getChunk().getPersistentDataContainer();
        NamespacedKey key = key(opibus, block);
        PersistentDataContainer data = chunk.getOrDefault(key, PersistentDataType.TAG_CONTAINER, chunk.getAdapterContext().newPersistentDataContainer());
        data.set(new NamespacedKey(opibus, "wasPlaced"), PersistentDataType.BOOLEAN, true);
        chunk.set(key, PersistentDataType.TAG_CONTAINER, data);
    }

    static void clear(Block block) {
        Plugin opibus = Bukkit.getPluginManager().getPlugin("Opibus");
        if (opibus != null) block.getChunk().getPersistentDataContainer().remove(key(opibus, block));
    }

    private static NamespacedKey key(Plugin opibus, Block block) {
        return new NamespacedKey(opibus, "x" + (block.getX() & 15) + "y" + block.getY() + "z" + (block.getZ() & 15));
    }
}
