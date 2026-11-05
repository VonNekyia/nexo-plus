package de.mcterranova.nexowatercrops;

import com.nexomc.nexo.api.events.NexoMechanicsRegisteredEvent;
import com.nexomc.nexo.mechanics.MechanicsManager;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Crops in water as plain blocks, without a furniture entity per plant: {@code water_crop} on a seed item in Nexo, the
 * looks on free states of kelp, see {@link KelpHost}.
 */
public class NexoWaterCrops extends JavaPlugin {

    @Override
    public void onEnable() {
        PluginManager plugins = getServer().getPluginManager();
        // Nexo fires this on start and on every reload of its items, as in NexoExampleMechanic
        plugins.registerEvents(new Listener() {
            @EventHandler
            public void onMechanics(NexoMechanicsRegisteredEvent event) {
                MechanicsManager.INSTANCE.registerMechanicFactory(new WaterCropFactory(), true);
            }
        }, this);
        Crops crops = new Crops(new KelpHost(), new CropRegistry(new NamespacedKey(this, "kelp_crops")), this);
        KelpGuard guard = new KelpGuard(crops, this);
        plugins.registerEvents(guard, this);
        plugins.registerEvents(new CropListener(crops, this), this);
        getServer().getScheduler().runTaskTimer(this, new GrowthTask(crops), 20, 20);
        for (World world : getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) guard.loaded(chunk);
        }
    }
}
