package de.mcterranova.nexowatercrops;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import io.papermc.paper.event.player.PlayerPickBlockEvent;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.type.Dispenser;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** The crop as players and the world meet it: planting, bone meal, breaking, picking, pistons and explosions. */
public class CropListener implements Listener {

    private final Crops crops;
    private final Plugin plugin;

    CropListener(Crops crops, Plugin plugin) {
        this.crops = crops;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        WaterCropMechanic crop = WaterCropFactory.crop();
        Block clicked = event.getClickedBlock();
        EquipmentSlot hand = event.getHand();
        if (crop == null || clicked == null || hand == null || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getPlayer().getInventory().getItem(hand);
        if (item.getType() == Material.BONE_MEAL) {
            int look = crops.look(clicked);
            if (look >= 0) boneMeal(event, clicked, look, crop, item);
        } else if (crop.getItemID().equals(NexoItems.idFromItem(item))) {
            plant(event, clicked, crop, item, hand);
        }
    }

    private void boneMeal(PlayerInteractEvent event, Block block, int look, WaterCropMechanic crop, ItemStack item) {
        boolean denied = event.useItemInHand() == Event.Result.DENY; // the protection of the region forbids it
        event.setUseItemInHand(Event.Result.DENY); // never vanilla's, which would grow kelp with water above
        if (denied || crop.stages().isRipe(look)) return;
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.CREATIVE) item.subtract();
        player.swingHand(event.getHand());
        grow(block, look, crop);
    }

    private void grow(Block block, int look, WaterCropMechanic crop) {
        int steps = ThreadLocalRandom.current().nextInt(crop.boneMealMin(), crop.boneMealMax() + 1);
        crops.host.show(block, crop.stages().grown(look, steps, crops.registry.chain(block)));
        block.getWorld().playEffect(block.getLocation(), Effect.BONE_MEAL_USE, 3);
    }

    private void plant(PlayerInteractEvent event, Block clicked, WaterCropMechanic crop, ItemStack item, EquipmentSlot hand) {
        Player player = event.getPlayer();
        if (event.useItemInHand() == Event.Result.DENY || clicked.getType().isInteractable() && !player.isSneaking()) return;
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        Block target = clicked.getRelative(event.getBlockFace());
        if (!crops.host.canPlant(target, crop)) {
            if (crop.wrongPlaceMessage() != null) player.sendMessage(crop.wrongPlaceMessage());
            return;
        }
        BlockState replaced = target.getState();
        crops.put(target, 0, ThreadLocalRandom.current().nextInt(crop.stages().ripe()));
        // protections and logs learn of it like of any placed block
        BlockPlaceEvent place = new BlockPlaceEvent(target, replaced, clicked, item, player, true, hand);
        if (!place.callEvent() || !place.canBuild()) {
            crops.forget(target);
            replaced.update(true, false);
            return;
        }
        if (player.getGameMode() != GameMode.CREATIVE) item.subtract();
        player.swingHand(hand);
        if (crop.placeSound() != null) target.getWorld().playSound(target.getLocation().toCenterLocation(), crop.placeSound(), 1, 1);
    }

    // a dispenser's bone meal like a player's; a ripe crop leaves the bone meal in the dispenser
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        WaterCropMechanic crop = WaterCropFactory.crop();
        Block source = event.getBlock();
        if (crop == null || event.getItem().getType() != Material.BONE_MEAL || source.getType() != Material.DISPENSER
                || !(source.getBlockData() instanceof Dispenser dispenser)) return;
        Block target = source.getRelative(dispenser.getFacing());
        int look = crops.look(target);
        if (look < 0) return;
        event.setCancelled(true);
        if (crop.stages().isRipe(look)) return;
        ItemStack one = event.getItem().asOne();
        // a tick later: the dispenser is still taking the item out while the event runs
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (source.getState() instanceof Container container) container.getInventory().removeItem(one);
        });
        grow(target, look, crop);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        int look = crops.look(block);
        if (look < 0) return;
        event.setDropItems(false);
        event.setExpToDrop(0);
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.CREATIVE) crops.dropLoot(block, look, player.getInventory().getItemInMainHand());
        crops.forgetLater(block);
    }

    // the ground under it went
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDestroy(BlockDestroyEvent event) {
        Block block = event.getBlock();
        int look = crops.look(block);
        if (look < 0) return;
        event.setWillDrop(false);
        crops.dropLoot(block, look, ItemStack.empty());
        crops.forgetLater(block);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        explode(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(BlockExplodeEvent event) {
        explode(event.blockList());
    }

    // out of the explosion, which would drop the host; the crop's loot falls and the water stays
    private void explode(List<Block> blocks) {
        blocks.removeIf(block -> {
            int look = crops.look(block);
            if (look < 0) return false;
            crops.dropLoot(block, look, ItemStack.empty());
            crops.forget(block);
            block.setType(Material.WATER);
            return true;
        });
    }

    // the blocks a piston would break are in the list too
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPiston(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> crops.look(block) >= 0)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPiston(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> crops.look(block) >= 0)) event.setCancelled(true);
    }

    // like vanilla's pick block, with the seed in place of the host
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPick(PlayerPickBlockEvent event) {
        WaterCropMechanic crop = WaterCropFactory.crop();
        if (crop == null || crops.look(event.getBlock()) < 0) return;
        event.setCancelled(true);
        PlayerInventory inventory = event.getPlayer().getInventory();
        int target = event.getTargetSlot();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.isEmpty() || !crop.getItemID().equals(NexoItems.idFromItem(stack))) continue;
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
        ItemBuilder item = NexoItems.itemFromId(crop.getItemID());
        if (item == null) return;
        ItemStack replaced = inventory.getItem(target);
        int free = inventory.firstEmpty();
        if (replaced != null && !replaced.isEmpty() && free != -1) inventory.setItem(free, replaced);
        inventory.setItem(target, item.build());
        inventory.setHeldItemSlot(target);
    }
}
