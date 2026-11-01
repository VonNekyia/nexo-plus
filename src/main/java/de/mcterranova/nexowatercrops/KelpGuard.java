package de.mcterranova.nexowatercrops;

import com.nexomc.nexo.api.events.resourcepack.NexoPrePackGenerateEvent;
import net.kyori.adventure.key.Key;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import team.unnamed.creative.blockstate.BlockState;
import team.unnamed.creative.blockstate.MultiVariant;
import team.unnamed.creative.blockstate.Variant;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Keeps the ages of the looks free of other kelp and kelp's own behaviour away from the crops. Natural kelp starts at
 * age 20 to 23 and grows upward from there, so only kelp placed by players reaches the low ages: it gets 10 more, in
 * the place event and once per chunk for what was placed before this plugin. Such kelp grows up to 10 blocks less.
 */
public class KelpGuard implements Listener {

    private static final int RAISE = 10;
    private static final NamespacedKey CHECKED = new NamespacedKey("nexowatercrops", "kelp_checked");

    private final Crops crops;
    private final Plugin plugin;

    KelpGuard(Crops crops, Plugin plugin) {
        this.crops = crops;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType() != Material.KELP || crops.registry.chain(block) >= 0) return;
        // kelp on a crop would turn the crop into a kelp stem
        if (crops.look(block.getRelative(BlockFace.DOWN)) >= 0) {
            event.setCancelled(true);
            return;
        }
        raise(block);
    }

    private static void raise(Block block) {
        Ageable kelp = (Ageable) block.getBlockData();
        if (kelp.getAge() >= KelpHost.LOOKS) return;
        kelp.setAge(kelp.getAge() + RAISE);
        block.setBlockData(kelp, false);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        loaded(event.getChunk());
    }

    void loaded(Chunk chunk) {
        crops.registry.chunkLoaded(chunk);
        if (chunk.getPersistentDataContainer().has(CHECKED, PersistentDataType.BOOLEAN)) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (chunk.isLoaded()) check(chunk);
        });
    }

    private void check(Chunk chunk) {
        for (int age = 0; age < KelpHost.LOOKS; age++) {
            if (chunk.contains(KelpHost.age(age))) {
                raiseOldKelp(chunk);
                break;
            }
        }
        chunk.getPersistentDataContainer().set(CHECKED, PersistentDataType.BOOLEAN, true);
    }

    private void raiseOldKelp(Chunk chunk) {
        ChunkSnapshot snapshot = chunk.getChunkSnapshot(false, false, false);
        int minY = chunk.getWorld().getMinHeight();
        int maxY = chunk.getWorld().getMaxHeight();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    if (snapshot.getBlockType(x, y, z) != Material.KELP) continue;
                    Block block = chunk.getBlock(x, y, z);
                    if (crops.registry.chain(block) < 0) raise(block);
                }
            }
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        crops.registry.chunkUnloaded(event.getChunk());
    }

    // water poured above a crop would let the kelp grow into it
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) {
        if (crops.look(event.getSource()) >= 0) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent event) {
        if (crops.look(event.getBlock()) >= 0) event.setCancelled(true);
    }

    // a sponge destroys kelp
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSponge(SpongeAbsorbEvent event) {
        event.getBlocks().removeIf(state -> crops.look(state.getBlock()) >= 0);
    }

    // through the pack's own API: Nexo renames models when it obfuscates and sorts their textures into the block atlas
    // by what the blockstates use, which a file added as unknown would miss
    @EventHandler
    public void onPack(NexoPrePackGenerateEvent event) {
        WaterCropMechanic crop = WaterCropFactory.crop();
        if (crop == null) return;
        Map<String, MultiVariant> variants = new LinkedHashMap<>();
        KelpHost.modelsByAge(crop.looks()).forEach((state, model) ->
                variants.put(state, MultiVariant.of(Variant.builder().model(Key.key(model)).build())));
        event.getResourcePack().blockState(BlockState.of(Key.key("minecraft:kelp"), variants));
    }
}
