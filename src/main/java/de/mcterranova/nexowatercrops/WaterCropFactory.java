package de.mcterranova.nexowatercrops;

import com.nexomc.nexo.mechanics.Mechanic;
import com.nexomc.nexo.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

public class WaterCropFactory extends MechanicFactory {

    // Nexo makes a new factory on every reload of its items; the listeners always ask the newest
    private static volatile WaterCropFactory instance;

    public WaterCropFactory() {
        super("water_crop");
        instance = this;
    }

    @Nullable
    public static WaterCropFactory instance() {
        return instance;
    }

    @Override
    @Nullable
    public WaterCropMechanic getMechanic(String itemId) {
        return (WaterCropMechanic) super.getMechanic(itemId);
    }

    @Override
    public Mechanic parse(ConfigurationSection section) {
        WaterCropMechanic mechanic = new WaterCropMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }
}
