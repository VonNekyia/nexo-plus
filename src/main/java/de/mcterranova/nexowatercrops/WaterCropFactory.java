package de.mcterranova.nexowatercrops;

import com.nexomc.nexo.mechanics.Mechanic;
import com.nexomc.nexo.mechanics.MechanicFactory;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

public class WaterCropFactory extends MechanicFactory {

    // Nexo makes a new factory on every reload of its items; the listeners always ask the newest
    private static volatile WaterCropFactory instance;

    @Nullable private WaterCropMechanic crop;

    public WaterCropFactory() {
        super("water_crop");
        instance = this;
    }

    /** The crop on kelp. Kelp has one set of looks, so there is one crop on it. */
    @Nullable
    public static WaterCropMechanic crop() {
        return instance == null ? null : instance.crop;
    }

    @Override
    @Nullable
    public Mechanic parse(ConfigurationSection section) {
        WaterCropMechanic mechanic = new WaterCropMechanic(this, section);
        if (mechanic.looks().size() > KelpHost.LOOKS || mechanic.stages().growing() == 0 || mechanic.stages().ripe() == 0) {
            Bukkit.getLogger().severe("[NexoWaterCrops] " + mechanic.getItemID() + " needs stages and ripe, at most " + KelpHost.LOOKS + " models together");
            return null;
        }
        if (crop != null) {
            Bukkit.getLogger().severe("[NexoWaterCrops] " + mechanic.getItemID() + " ignored: " + crop.getItemID() + " already is the crop on kelp");
            return null;
        }
        crop = mechanic;
        addToImplemented(mechanic);
        return mechanic;
    }
}
