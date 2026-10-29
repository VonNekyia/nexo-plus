package de.mcterranova.nexowatercrops;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Keeps a host under every water crop. Nexo spawns a new furniture entity for planting and for every stage of its
 * evolution, and removes the old one, so following the entities covers planting, growing, harvesting and breaking.
 * Each step waits a tick, until Nexo is done with the entities and the world may change.
 */
public class CropLifecycle implements Listener {

    private final Plugin plugin;

    public CropLifecycle(Plugin plugin) {
        this.plugin = plugin;
    }

    // also when a chunk loads: crops planted before this plugin get their host then
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdd(EntityAddToWorldEvent event) {
        if (Hosts.mechanic(event.getEntity()) == null) return;
        Block block = event.getEntity().getLocation().getBlock();
        plugin.getServer().getScheduler().runTask(plugin, () -> Hosts.ensure(block));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRemove(EntityRemoveEvent event) {
        if (event.getCause() == EntityRemoveEvent.Cause.UNLOAD || event.getCause() == EntityRemoveEvent.Cause.PLAYER_QUIT) return;
        if (Hosts.mechanic(event.getEntity()) == null) return;
        Block block = event.getEntity().getLocation().getBlock();
        // a stage swap has placed the next stage by then, which keeps the host
        plugin.getServer().getScheduler().runTask(plugin, () -> Hosts.release(block));
    }

    // the entities are there now, so a host without its crop shows
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        List<Block> hosts = Hosts.noted(event.getChunk());
        if (hosts.isEmpty()) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> hosts.forEach(Hosts::release));
    }
}
