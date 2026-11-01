package de.mcterranova.nexowatercrops;

import org.bukkit.block.Block;

/**
 * A vanilla block with states nothing else uses, which the pack turns into the looks of a crop. {@link KelpHost} for
 * crops in water; a host for land crops would come in beside it with its own {@link CropRegistry}.
 */
interface CropHost {

    /** The look this block shows, -1 if it is not this host in a state of a look. */
    int look(Block block);

    void show(Block block, int look);

    /** Whether the crop may be planted here. */
    boolean canPlant(Block block, WaterCropMechanic crop);

    /** Whether a crop can stand here at all, whatever its rules for planting. */
    boolean canStand(Block block);
}
