package de.mcterranova.nexowatercrops;

import com.nexomc.nexo.mechanics.Mechanic;
import com.nexomc.nexo.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

/**
 * {@code water_crop} on a Nexo furniture item: the furniture stands in a water source and a host block under it gives
 * the water a real block outline the player can aim at.
 * <ul>
 *     <li>{@code pick_item}: the Nexo item a middle click gives, by default the furniture's own item</li>
 *     <li>{@code particle}: the texture of the break particles; the host block is one for all water crops, so the
 *     first item that sets it wins</li>
 * </ul>
 */
public class WaterCropMechanic extends Mechanic {

    private final String pickItem;
    @Nullable private final String particle;

    public WaterCropMechanic(MechanicFactory factory, ConfigurationSection section) {
        super(factory, section);
        pickItem = section.getString("pick_item", getItemID());
        particle = section.getString("particle");
    }

    public String pickItem() {
        return pickItem;
    }

    @Nullable
    public String particle() {
        return particle;
    }
}
