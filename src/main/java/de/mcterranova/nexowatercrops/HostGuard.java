package de.mcterranova.nexowatercrops;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import com.nexomc.nexo.items.ItemBuilder;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import com.nexomc.nexo.mechanics.furniture.evolution.EvolvingFurniture;
import io.papermc.paper.event.player.PlayerPickBlockEvent;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.type.Dispenser;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Makes the host act as the crop: breaking, clicking and picking it go to the Nexo furniture, and nothing in the
 * world ever gets the propagule itself, its drop or its tree.
 */
public class HostGuard implements Listener {

    private final Plugin plugin;

    public HostGuard(Plugin plugin) {
        this.plugin = plugin;
    }

    // the same order as Nexo's own hitboxes: the block break first, then the furniture break with Nexo's loot
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!Hosts.isHost(block)) return;
        event.setDropItems(false);
        event.setExpToDrop(0);
        plugin.getServer().getScheduler().runTask(plugin, () -> Hosts.release(block));
        ItemDisplay crop = Hosts.crop(block);
        FurnitureMechanic furniture = crop == null ? null : NexoFurniture.furnitureMechanic(crop);
        if (furniture == null) return;
        NexoFurnitureBreakEvent breaking = new NexoFurnitureBreakEvent(furniture, crop, event.getPlayer());
        if (!breaking.callEvent()) {
            event.setCancelled(true);
            return;
        }
        NexoFurniture.remove(crop, event.getPlayer(), breaking.getDrop());
    }

    // the ground under it went; the crop drops its loot like wheat on broken farmland
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDestroy(BlockDestroyEvent event) {
        Block block = event.getBlock();
        if (!Hosts.isHost(block)) return;
        event.setWillDrop(false);
        ItemDisplay crop = Hosts.crop(block);
        if (crop != null) Hosts.dropAndRemove(crop);
        plugin.getServer().getScheduler().runTask(plugin, () -> Hosts.release(block));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        explode(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(BlockExplodeEvent event) {
        explode(event.blockList());
    }

    // out of the explosion, which would drop the propagule; the crop drops and the water stays
    private static void explode(List<Block> blocks) {
        blocks.removeIf(block -> {
            if (!Hosts.isHost(block)) return false;
            ItemDisplay crop = Hosts.crop(block);
            if (crop != null) Hosts.dropAndRemove(crop);
            Hosts.toWater(block);
            return true;
        });
    }

    // a click on the host is a click on the furniture, like on Nexo's own hitboxes; vanilla bone meal never reaches it
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() == null) return;
        Block block = event.getClickedBlock();
        if (!Hosts.isHost(block)) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getHand());
        ItemDisplay crop = Hosts.crop(block);
        FurnitureMechanic furniture = crop == null ? null : NexoFurniture.furnitureMechanic(crop);
        if (furniture != null) {
            Location point = event.getInteractionPoint() != null ? event.getInteractionPoint() : block.getLocation().toCenterLocation();
            NexoFurnitureInteractEvent interact = new NexoFurnitureInteractEvent(furniture, crop, player, item,
                    event.getHand(), point, event.useInteractedBlock(), event.useItemInHand(), event.getBlockFace());
            if (interact.callEvent()) {
                event.setUseInteractedBlock(interact.getUseFurniture());
                event.setUseItemInHand(interact.getUseItemInHand());
            } else {
                event.setUseInteractedBlock(Event.Result.DENY);
                event.setUseItemInHand(Event.Result.DENY);
            }
        }
        if (item.getType() == Material.BONE_MEAL) event.setUseItemInHand(Event.Result.DENY);
    }

    // a dispenser's bone meal grows one stage like Nexo's; TerranovaLib grows rice its own way before this and cancels
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        Block dispenserBlock = event.getBlock();
        if (event.getItem().getType() != Material.BONE_MEAL || dispenserBlock.getType() != Material.DISPENSER
                || !(dispenserBlock.getBlockData() instanceof Dispenser dispenser)) return;
        Block target = dispenserBlock.getRelative(dispenser.getFacing());
        if (!Hosts.isHost(target)) return;
        event.setCancelled(true);
        ItemDisplay crop = Hosts.crop(target);
        FurnitureMechanic furniture = crop == null ? null : NexoFurniture.furnitureMechanic(crop);
        EvolvingFurniture evolution = furniture == null ? null : furniture.getEvolution();
        // fully grown: the dispenser keeps its bone meal, as with grown wheat
        if (evolution == null || evolution.getNextStage() == null || NexoFurniture.furnitureMechanic(evolution.getNextStage()) == null) return;
        ItemStack one = event.getItem().asOne();
        // a tick later: the dispenser is still taking the item out while the event runs
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (dispenserBlock.getState() instanceof Container container) container.getInventory().removeItem(one);
        });
        crop.getWorld().playEffect(crop.getLocation(), Effect.BONE_MEAL_USE, 3);
        // the same chance check as Nexo's bone meal on furniture
        if (ThreadLocalRandom.current().nextDouble() > evolution.getBoneMealChance()) return;
        Location at = crop.getLocation().toBlockLocation();
        float yaw = crop.getLocation().getYaw();
        NexoFurniture.remove(crop);
        NexoFurniture.place(evolution.getNextStage(), at, yaw, BlockFace.UP);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent event) {
        if (Hosts.isHost(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGrow(StructureGrowEvent event) {
        if (Hosts.isHost(event.getLocation().getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucket(PlayerBucketFillEvent event) {
        if (Hosts.isHost(event.getBlockClicked()) || Hosts.isHost(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSponge(SpongeAbsorbEvent event) {
        event.getBlocks().removeIf(state -> Hosts.isHost(state.getBlock()));
    }

    // the blocks a piston would break are in the list too
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPiston(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(Hosts::isHost)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPiston(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(Hosts::isHost)) event.setCancelled(true);
    }

    // like vanilla's pick block, with the crop's item in place of the propagule
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPick(PlayerPickBlockEvent event) {
        if (!Hosts.isHost(event.getBlock())) return;
        event.setCancelled(true);
        ItemDisplay crop = Hosts.crop(event.getBlock());
        WaterCropMechanic mechanic = crop == null ? null : Hosts.mechanic(crop);
        if (mechanic == null) return;
        PlayerInventory inventory = event.getPlayer().getInventory();
        int target = event.getTargetSlot();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.isEmpty() || !mechanic.pickItem().equals(NexoItems.idFromItem(stack))) continue;
            if (slot < 9) {
                inventory.setHeldItemSlot(slot);
            } else {
                inventory.setItem(slot, inventory.getItem(target));
                inventory.setItem(target, stack);
                inventory.setHeldItemSlot(target);
            }
            return;
        }
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) return;
        ItemBuilder item = NexoItems.itemFromId(mechanic.pickItem());
        if (item == null) return;
        ItemStack replaced = inventory.getItem(target);
        int free = inventory.firstEmpty();
        if (replaced != null && !replaced.isEmpty() && free != -1) inventory.setItem(free, replaced);
        inventory.setItem(target, item.build());
        inventory.setHeldItemSlot(target);
    }
}
