package de.mcterranova.nexowatercrops;

import com.nexomc.nexo.mechanics.Mechanic;
import com.nexomc.nexo.mechanics.MechanicFactory;
import com.nexomc.nexo.utils.drops.Drop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * {@code water_crop} on the seed item of a crop that stands in water as a plain block:
 * <ul>
 *     <li>{@code stages}: the models of the growing stages, the first one when just planted</li>
 *     <li>{@code ripe}: the models of the ripe stage, one for each chain; the chain is rolled at planting</li>
 *     <li>{@code ground}: the blocks it may be planted on</li>
 *     <li>{@code growth}: {@code delay_seconds} between tries, {@code probability} of a stage per try,
 *     {@code minimum_light_level} below which it does not grow, and {@code light_boost} with its own
 *     {@code minimum_light_level} and {@code probability} in brighter light</li>
 *     <li>{@code bone_meal}: {@code min_stages} and {@code max_stages} one bone meal grows it</li>
 *     <li>{@code drop}, {@code ripe_drop}: what breaking it drops, in the format of Nexo's {@code drop}</li>
 *     <li>{@code place_sound}, {@code wrong_place_message}: optional</li>
 * </ul>
 */
public class WaterCropMechanic extends Mechanic {

    private final List<String> looks = new ArrayList<>();
    private final CropStages stages;
    private final Set<Material> ground = EnumSet.noneOf(Material.class);
    private final int delaySeconds;
    private final double probability;
    private final int minimumLight;
    private final double boostProbability;
    private final int boostLight;
    private final int boneMealMin;
    private final int boneMealMax;
    private final Drop drop;
    private final Drop ripeDrop;
    @Nullable private final String placeSound;
    @Nullable private final Component wrongPlaceMessage;

    public WaterCropMechanic(MechanicFactory factory, ConfigurationSection section) {
        super(factory, section);
        List<String> growing = section.getStringList("stages");
        List<String> ripe = section.getStringList("ripe");
        looks.addAll(growing);
        looks.addAll(ripe);
        stages = new CropStages(growing.size(), ripe.size());
        section.getStringList("ground").forEach(name -> ground.add(Material.valueOf(name)));

        ConfigurationSection growth = section.getConfigurationSection("growth");
        delaySeconds = growth == null ? 60 : Math.max(1, growth.getInt("delay_seconds", 60));
        probability = growth == null ? 0 : growth.getDouble("probability");
        minimumLight = growth == null ? 0 : growth.getInt("minimum_light_level");
        ConfigurationSection boost = growth == null ? null : growth.getConfigurationSection("light_boost");
        boostProbability = boost == null ? probability : boost.getDouble("probability");
        boostLight = boost == null ? Integer.MAX_VALUE : boost.getInt("minimum_light_level");

        boneMealMin = section.getInt("bone_meal.min_stages", 1);
        boneMealMax = Math.max(boneMealMin, section.getInt("bone_meal.max_stages", boneMealMin));
        drop = drop(section, "drop");
        ripeDrop = section.isConfigurationSection("ripe_drop") ? drop(section, "ripe_drop") : drop;
        placeSound = section.getString("place_sound");
        String message = section.getString("wrong_place_message");
        wrongPlaceMessage = message == null ? null : MiniMessage.miniMessage().deserialize(message);
    }

    private Drop drop(ConfigurationSection section, String name) {
        ConfigurationSection dropSection = section.getConfigurationSection(name);
        return dropSection == null ? Drop.emptyDrop() : Drop.createDrop(List.of(), dropSection, getItemID());
    }

    /** The models in the order of the looks. */
    List<String> looks() {
        return looks;
    }

    CropStages stages() {
        return stages;
    }

    Set<Material> ground() {
        return ground;
    }

    int delaySeconds() {
        return delaySeconds;
    }

    /** The highest chance of a stage per try, whatever the light. */
    double bestChance() {
        return Math.max(probability, boostProbability);
    }

    double chance(int light) {
        if (light >= boostLight) return boostProbability;
        return light >= minimumLight ? probability : 0;
    }

    int boneMealMin() {
        return boneMealMin;
    }

    int boneMealMax() {
        return boneMealMax;
    }

    Drop drop(int look) {
        return stages.isRipe(look) ? ripeDrop : drop;
    }

    @Nullable
    String placeSound() {
        return placeSound;
    }

    @Nullable
    Component wrongPlaceMessage() {
        return wrongPlaceMessage;
    }
}
