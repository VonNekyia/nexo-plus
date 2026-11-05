package de.mcterranova.nexowatercrops;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Grows the crops. Runs once a second over a share of the chunks with crops, so each chunk has its turn once per
 * {@code delay_seconds} and large fields do not all grow in one tick. The dice come first: only a crop that wins them
 * reads its block.
 */
final class GrowthTask implements Runnable {

    private final Crops crops;
    private int turn;

    GrowthTask(Crops crops) {
        this.crops = crops;
    }

    @Override
    public void run() {
        WaterCropMechanic crop = WaterCropFactory.crop();
        if (crop == null) return;
        int shares = crop.delaySeconds();
        turn = (turn + 1) % shares;
        for (Chunk chunk : List.copyOf(crops.registry.chunks())) {
            if (Math.floorMod(chunk.getX() * 31 + chunk.getZ(), shares) == turn) grow(chunk, crop);
        }
    }

    private void grow(Chunk chunk, WaterCropMechanic crop) {
        World world = chunk.getWorld();
        int[] entries = crops.registry.entries(chunk);
        double best = crop.bestChance();
        List<Block> gone = new ArrayList<>();
        for (int i = 0; i + CropRegistry.ENTRY <= entries.length; i += CropRegistry.ENTRY) {
            double roll = ThreadLocalRandom.current().nextDouble();
            if (roll >= best) continue;
            Block block = world.getBlockAt(entries[i], entries[i + 1], entries[i + 2]);
            int look = crops.host.look(block);
            if (look < 0) {
                gone.add(block); // WorldEdit or the like took it away
            } else if (!crop.stages().isRipe(look) && roll < crop.chance(block.getLightLevel())) {
                crops.host.show(block, crop.stages().grown(look, 1, entries[i + 3]));
            }
        }
        gone.forEach(crops::forget);
    }
}
