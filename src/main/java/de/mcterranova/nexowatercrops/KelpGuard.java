package de.mcterranova.nexowatercrops;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import com.nexomc.nexo.api.events.resourcepack.NexoPrePackGenerateEvent;
import net.kyori.adventure.key.Key;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;
import team.unnamed.creative.blockstate.BlockState;
import team.unnamed.creative.blockstate.MultiVariant;
import team.unnamed.creative.blockstate.Variant;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Keeps the ages of the looks free of other kelp and kelp's own behaviour away from the crops. Kelp gets to a low age
 * in three ways, and gets 10 more for each, so it grows up to 10 blocks less:
 * <ul>
 *     <li>a player places it: in the place event</li>
 *     <li>the kelp above a stem goes: the stem turns into a tip of a random age, without an event of its own
 *     (GrowingPlantBodyBlock#updateShape), so the events that take kelp away look at the block below a tick later</li>
 *     <li>anything else, as structures or other plugins: when the chunk loads</li>
 * </ul>
 */
public class KelpGuard implements Listener {

    private static final int RAISE = 10;

    private final Crops crops;
    private final Plugin plugin;
    private final Queue<Chunk> toScan = new ArrayDeque<>();
    private final BlockData[] lowAges = new BlockData[KelpHost.LOOKS];

    KelpGuard(Crops crops, Plugin plugin) {
        this.crops = crops;
        this.plugin = plugin;
        for (int age = 0; age < lowAges.length; age++) lowAges[age] = KelpHost.age(age);
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

    // last, so these see what really goes: the crops are out of the explosions, and pistons at crops are cancelled

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        gone(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDestroy(BlockDestroyEvent event) {
        gone(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        allGone(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(BlockExplodeEvent event) {
        allGone(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPiston(BlockPistonExtendEvent event) {
        allGone(event.getBlocks());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPiston(BlockPistonRetractEvent event) {
        allGone(event.getBlocks());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpongeAbsorbed(SpongeAbsorbEvent event) {
        event.getBlocks().forEach(state -> gone(state.getBlock()));
    }

    // a wither, for one
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityChange(EntityChangeBlockEvent event) {
        gone(event.getBlock());
    }

    private void allGone(Collection<Block> blocks) {
        blocks.forEach(this::gone);
    }

    private void gone(Block block) {
        if (block.getType() != Material.KELP && block.getType() != Material.KELP_PLANT) return;
        Block below = block.getRelative(BlockFace.DOWN);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (below.getType() == Material.KELP && crops.registry.chain(below) < 0) raise(below);
        });
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        loaded(event.getChunk());
    }

    void loaded(Chunk chunk) {
        crops.registry.chunkLoaded(chunk);
        for (BlockData lowAge : lowAges) {
            if (chunk.contains(lowAge)) {
                if (toScan.isEmpty()) plugin.getServer().getScheduler().runTask(plugin, this::scanNext);
                toScan.add(chunk);
                return;
            }
        }
    }

    // ponytail: the API tells only per chunk whether a state may be in it, and a chunk with crops always may. So it reads
    // the whole chunk but for empty sections, one chunk per tick; asking per section would take server internals.
    private void scanNext() {
        Chunk chunk = toScan.poll();
        if (chunk != null && chunk.isLoaded()) raiseStrayKelp(chunk);
        if (!toScan.isEmpty()) plugin.getServer().getScheduler().runTask(plugin, this::scanNext);
    }

    private void raiseStrayKelp(Chunk chunk) {
        int[] entries = crops.registry.entries(chunk);
        Set<Integer> planted = new HashSet<>();
        for (int i = 0; i + CropRegistry.ENTRY <= entries.length; i += CropRegistry.ENTRY) {
            planted.add(position(entries[i] & 15, entries[i + 1], entries[i + 2] & 15));
        }
        ChunkSnapshot snapshot = chunk.getChunkSnapshot(false, false, false);
        int minY = chunk.getWorld().getMinHeight();
        int sections = (chunk.getWorld().getMaxHeight() - minY) >> 4;
        for (int section = 0; section < sections; section++) {
            if (snapshot.isSectionEmpty(section)) continue;
            int fromY = minY + (section << 4);
            for (int y = fromY; y < fromY + 16; y++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        if (snapshot.getBlockType(x, y, z) != Material.KELP) continue;
                        BlockData kelp = snapshot.getBlockData(x, y, z);
                        if (((Ageable) kelp).getAge() < KelpHost.LOOKS && !planted.contains(position(x, y, z))) raise(chunk.getBlock(x, y, z));
                    }
                }
            }
        }
    }

    private static int position(int x, int y, int z) {
        return y << 8 | x << 4 | z;
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
