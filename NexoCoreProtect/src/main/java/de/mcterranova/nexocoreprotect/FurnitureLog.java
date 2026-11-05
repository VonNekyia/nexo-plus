package de.mcterranova.nexocoreprotect;

import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureDamageEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurniturePlaceEvent;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import net.coreprotect.config.ConfigHandler;
import net.coreprotect.consumer.Queue;
import net.coreprotect.listener.player.inspector.BlockInspector;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Logs furniture as a block named nexo:&lt;item id&gt; at the block of its base entity, so it shows in lookups and the
 * inspector under that name. CoreProtect's API takes only real block data, so this goes through its queue, which is
 * protected, hence the subclass.
 * <p>
 * A rollback leaves the furniture alone: for a removal CoreProtect finds no block of that name to place and skips it,
 * for a placement it sets the block to air, which it already is under furniture without barriers.
 */
final class FurnitureLog extends Queue implements Listener {

    private final BlockInspector inspector = new BlockInspector();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(NexoFurniturePlaceEvent event) {
        Block block = block(event.getBaseEntity());
        queueBlockPlace(event.getPlayer().getName(), block.getState(), block.getType(), null, null, -1, 0, key(event.getMechanic()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(NexoFurnitureBreakEvent event) {
        Block block = block(event.getBaseEntity());
        queueBlockBreak(event.getPlayer().getName(), block.getState(), block.getType(), key(event.getMechanic()), 0);
    }

    // CoreProtect's inspector sees blocks, and the hitbox of furniture covers the block it stands on. So a click on the
    // furniture while inspecting shows the block of the furniture and does nothing else, as on a block.

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInspect(NexoFurnitureInteractEvent event) {
        inspect(event.getPlayer(), event.getBaseEntity(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInspect(NexoFurnitureDamageEvent event) {
        inspect(event.getPlayer(), event.getBaseEntity(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInspect(NexoFurnitureBreakEvent event) {
        inspect(event.getPlayer(), event.getBaseEntity(), event);
    }

    private void inspect(Player player, Entity base, Cancellable event) {
        if (!Boolean.TRUE.equals(ConfigHandler.inspecting.get(player.getName())) || !player.hasPermission("coreprotect.inspect")) return;
        event.setCancelled(true);
        inspector.performAirBlockLookup(player, block(base).getState());
    }

    private static Block block(Entity base) {
        return base.getLocation().getBlock();
    }

    private static String key(FurnitureMechanic mechanic) {
        return "nexo:" + mechanic.getItemID();
    }
}
