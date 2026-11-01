package de.mcterranova.nexowatercrops;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Crops in water on kelp: the block always holds a water source, has an outline of 16 x 9 x 16 and no collision. The
 * ages below {@link #LOOKS} are the looks; {@link KelpGuard} keeps all other kelp out of them. With air above, kelp
 * does not grow by itself.
 */
final class KelpHost implements CropHost {

    static final int LOOKS = 10;
    private static final int MAX_AGE = 25;

    @Override
    public int look(Block block) {
        if (block.getType() != Material.KELP) return -1;
        int age = ((Ageable) block.getBlockData()).getAge();
        return age < LOOKS ? age : -1;
    }

    @Override
    public void show(Block block, int look) {
        block.setBlockData(age(look), false);
    }

    @Override
    public boolean canPlant(Block block, WaterCropMechanic crop) {
        return canStand(block) && !isWater(block.getRelative(BlockFace.UP))
                && crop.ground().contains(block.getRelative(BlockFace.DOWN).getType());
    }

    @Override
    public boolean canStand(Block block) {
        return isSource(block) && age(0).isSupported(block);
    }

    static BlockData age(int age) {
        Ageable kelp = (Ageable) Material.KELP.createBlockData();
        kelp.setAge(age);
        return kelp;
    }

    /** The model of every age: the looks, then vanilla kelp. */
    static Map<String, String> modelsByAge(List<String> looks) {
        Map<String, String> models = new LinkedHashMap<>();
        for (int age = 0; age <= MAX_AGE; age++) {
            models.put("age=" + age, age < looks.size() ? looks.get(age) : "minecraft:block/kelp");
        }
        return models;
    }

    private static boolean isSource(Block block) {
        return block.getType() == Material.WATER && block.getBlockData() instanceof Levelled water && water.getLevel() == 0;
    }

    private static boolean isWater(Block block) {
        return block.getType() == Material.WATER
                || block.getBlockData() instanceof Waterlogged waterlogged && waterlogged.isWaterlogged();
    }
}
