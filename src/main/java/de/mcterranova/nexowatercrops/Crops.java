package de.mcterranova.nexowatercrops;

import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/** What the listeners and the growth pass share: a crop is a block of the host that the registry knows. */
final class Crops {

    final CropHost host;
    final CropRegistry registry;
    private final Plugin plugin;

    Crops(CropHost host, CropRegistry registry, Plugin plugin) {
        this.host = host;
        this.registry = registry;
        this.plugin = plugin;
    }

    /** The look of the crop in this block, -1 if there is none. */
    int look(Block block) {
        int look = host.look(block);
        return look >= 0 && registry.chain(block) >= 0 ? look : -1;
    }

    void put(Block block, int look, int chain) {
        host.show(block, look);
        registry.add(block, chain);
        OpibusMark.set(block);
    }

    void forget(Block block) {
        registry.remove(block);
        OpibusMark.clear(block);
    }

    /** A tick later, when every plugin has seen the break and Opibus has found its mark. */
    void forgetLater(Block block) {
        plugin.getServer().getScheduler().runTask(plugin, () -> forget(block));
    }

    void dropLoot(Block block, int look, ItemStack tool) {
        WaterCropMechanic crop = WaterCropFactory.crop();
        if (crop != null) crop.drop(look).spawns(block.getLocation().toCenterLocation(), tool);
    }
}
