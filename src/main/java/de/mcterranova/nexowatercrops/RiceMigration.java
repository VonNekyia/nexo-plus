package de.mcterranova.nexowatercrops;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the rice that still stands as Nexo furniture into blocks when its chunk loads. Reads the id itself, since the
 * items are gone from Nexo by then, and before Nexo gets to clear away furniture it no longer knows. Rice that cannot
 * stand as a block, as on farmland, drops its loot instead.
 * <p>
 * ponytail: rice only and for this one move; delete this class once main and farm have loaded their rice fields.
 */
public class RiceMigration implements Listener {

    private static final int RETRIES = 120;
    private static final Pattern STAGE = Pattern.compile("reis_(?:([bc])_)?stufe(\\d)");

    private final Crops crops;
    private final Plugin plugin;

    RiceMigration(Crops crops, Plugin plugin) {
        this.crops = crops;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAdd(EntityAddToWorldEvent event) {
        add(event.getEntity());
    }

    private void add(Entity entity) {
        if (!(entity instanceof ItemDisplay)) return;
        String id = entity.getPersistentDataContainer().get(FurnitureMechanic.Companion.getFURNITURE_KEY(), PersistentDataType.STRING);
        int[] stageAndChain = id == null ? null : stageAndChain(id);
        if (stageAndChain == null) return;
        Block block = entity.getLocation().getBlock();
        plugin.getServer().getScheduler().runTask(plugin, () -> convert(entity, block, stageAndChain[0], stageAndChain[1], RETRIES));
    }

    /** For furniture that loaded before this plugin started. */
    void convertLoaded(World world) {
        world.getEntitiesByClass(ItemDisplay.class).forEach(this::add);
    }

    private void convert(Entity entity, Block block, int stage, int chain, int retries) {
        WaterCropMechanic crop = WaterCropFactory.crop();
        if (crop == null) {
            // on start the chunks of the spawn can load before Nexo has read its items
            if (retries > 0) plugin.getServer().getScheduler().runTaskLater(plugin, () -> convert(entity, block, stage, chain, retries - 1), 20);
            return;
        }
        entity.remove();
        int look = crop.stages().look(stage, chain);
        if (crops.host.canStand(block)) crops.put(block, look, chain);
        else crops.dropLoot(block, look, ItemStack.empty());
    }

    /** Stage and chain of a rice furniture id, null if it is none. */
    @Nullable
    static int[] stageAndChain(String id) {
        if (id.equals("reis_saat")) return new int[]{0, 0};
        Matcher matcher = STAGE.matcher(id);
        if (!matcher.matches()) return null;
        int chain = matcher.group(1) == null ? 0 : matcher.group(1).equals("b") ? 1 : 2;
        return new int[]{Integer.parseInt(matcher.group(2)), chain};
    }
}
