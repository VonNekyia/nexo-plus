package de.mcterranova.nexowatercrops;

import com.nexomc.nexo.api.events.resourcepack.NexoPrePackGenerateEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

/**
 * Adds the host's look to Nexo's pack: the free propagule states get an empty model with the crop's particles, so only
 * the water of the block shows. Every vanilla state keeps its vanilla model.
 */
public class HostPack implements Listener {

    static final String BLOCKSTATE = "assets/minecraft/blockstates/mangrove_propagule.json";
    static final String MODEL = "assets/nexowatercrops/models/block/host.json";
    private static final String DEFAULT_PARTICLE = "minecraft:block/wheat_stage7";

    private final Plugin plugin;

    public HostPack(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPack(NexoPrePackGenerateEvent event) {
        if (!event.addUnknownFile(BLOCKSTATE, blockstate().getBytes(StandardCharsets.UTF_8))
                || !event.addUnknownFile(MODEL, model(particle()).getBytes(StandardCharsets.UTF_8))) {
            plugin.getLogger().warning("The pack has its own " + BLOCKSTATE + " or " + MODEL + "; water crops would show a propagule");
        }
    }

    private static String particle() {
        WaterCropFactory factory = WaterCropFactory.instance();
        if (factory == null) return DEFAULT_PARTICLE;
        for (String item : factory.items()) {
            WaterCropMechanic mechanic = factory.getMechanic(item);
            if (mechanic != null && mechanic.particle() != null) return mechanic.particle();
        }
        return DEFAULT_PARTICLE;
    }

    // vanilla's file with its age and hanging variants; only standing below age 4 is ours
    static String blockstate() {
        StringJoiner variants = new StringJoiner(",\n", "{\n  \"variants\": {\n", "\n  }\n}\n");
        for (int age = 0; age <= 4; age++) {
            String standing = age < 4 ? "nexowatercrops:block/host" : "minecraft:block/mangrove_propagule";
            variants.add(variant("age=" + age + ",hanging=false", standing));
            variants.add(variant("age=" + age + ",hanging=true", "minecraft:block/mangrove_propagule_hanging_" + age));
        }
        return variants.toString();
    }

    static String model(String particle) {
        return "{\n  \"textures\": {\n    \"particle\": \"" + particle + "\"\n  },\n  \"elements\": []\n}\n";
    }

    private static String variant(String state, String model) {
        return "    \"" + state + "\": {\n      \"model\": \"" + model + "\"\n    }";
    }
}
