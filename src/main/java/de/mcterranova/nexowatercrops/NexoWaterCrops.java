package de.mcterranova.nexowatercrops;

import com.nexomc.nexo.api.events.NexoMechanicsRegisteredEvent;
import com.nexomc.nexo.mechanics.MechanicsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Nexo furniture crops that stand in water: {@code water_crop} on a furniture item puts a host block under it, see
 * {@link Hosts}.
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
        plugins.registerEvents(new CropLifecycle(this), this);
        plugins.registerEvents(new HostGuard(this), this);
        plugins.registerEvents(new HostPack(this), this);
    }
}
