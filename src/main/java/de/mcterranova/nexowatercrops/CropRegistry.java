package de.mcterranova.nexowatercrops;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Where the crops stand: in each chunk's data one int array with x, y, z and the chain of each crop, so the growth pass
 * touches nothing but crops. Only chunks that hold crops are kept in memory.
 */
final class CropRegistry {

    static final int ENTRY = 4;

    private final NamespacedKey key;
    private final Set<Chunk> chunks = new HashSet<>();

    CropRegistry(NamespacedKey key) {
        this.key = key;
    }

    /** The loaded chunks that hold crops. */
    Set<Chunk> chunks() {
        return chunks;
    }

    void chunkLoaded(Chunk chunk) {
        if (chunk.getPersistentDataContainer().has(key, PersistentDataType.INTEGER_ARRAY)) chunks.add(chunk);
    }

    void chunkUnloaded(Chunk chunk) {
        chunks.remove(chunk);
    }

    int[] entries(Chunk chunk) {
        return chunk.getPersistentDataContainer().getOrDefault(key, PersistentDataType.INTEGER_ARRAY, new int[0]);
    }

    /** The chain of the crop in this block, -1 if there is none. */
    int chain(Block block) {
        int[] entries = entries(block.getChunk());
        int at = indexOf(entries, block);
        return at < 0 ? -1 : entries[at + 3];
    }

    void add(Block block, int chain) {
        Chunk chunk = block.getChunk();
        int[] entries = entries(chunk);
        int at = indexOf(entries, block);
        if (at >= 0) {
            entries[at + 3] = chain;
        } else {
            entries = Arrays.copyOf(entries, entries.length + ENTRY);
            at = entries.length - ENTRY;
            entries[at] = block.getX();
            entries[at + 1] = block.getY();
            entries[at + 2] = block.getZ();
            entries[at + 3] = chain;
        }
        chunk.getPersistentDataContainer().set(key, PersistentDataType.INTEGER_ARRAY, entries);
        chunks.add(chunk);
    }

    void remove(Block block) {
        Chunk chunk = block.getChunk();
        int[] entries = entries(chunk);
        int at = indexOf(entries, block);
        if (at < 0) return;
        PersistentDataContainer data = chunk.getPersistentDataContainer();
        if (entries.length == ENTRY) {
            data.remove(key);
            chunks.remove(chunk);
            return;
        }
        int[] changed = new int[entries.length - ENTRY];
        System.arraycopy(entries, 0, changed, 0, at);
        System.arraycopy(entries, at + ENTRY, changed, at, entries.length - at - ENTRY);
        data.set(key, PersistentDataType.INTEGER_ARRAY, changed);
    }

    private static int indexOf(int[] entries, Block block) {
        for (int i = 0; i + ENTRY <= entries.length; i += ENTRY) {
            if (entries[i] == block.getX() && entries[i + 1] == block.getY() && entries[i + 2] == block.getZ()) return i;
        }
        return -1;
    }
}
