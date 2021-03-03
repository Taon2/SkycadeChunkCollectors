package net.skycade.skycadechunkcollectors.listener;

import net.skycade.skycadechunkcollectors.data.BlockData;
import net.skycade.skycadechunkcollectors.data.ChunkCollectorManager;
import net.skycade.skycadechunkcollectors.gui.CollectorGui;
import net.skycade.skycadechunkcollectors.hook.Hook;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static net.skycade.skycadechunkcollectors.SkycadeChunkCollectorsPlugin.v112;
import static net.skycade.skycadechunkcollectors.SkycadeChunkCollectorsPlugin.v116;
import static net.skycade.skycadechunkcollectors.command.ChunkCollectorCommand.COLLECTOR;
import static net.skycade.skycadechunkcollectors.command.ChunkCollectorCommand.getCollectorLore;
import static net.skycade.skycadechunkcollectors.util.Messages.*;

public class ChunkCollectorListener implements Listener {

    /**
     * Handles placing a new chunk collector into the world
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();

        // if you cant build here, don't place here
        if (!Hook.checkBuild(event.getPlayer(), event.getBlock())) return;

        // stop eyes of ender from being placed in the end portal frames
        if (item.getType() == (v116 ? Material.ENDER_EYE : Material.valueOf("EYE_OF_ENDER"))) {
            BlockData blockData = ChunkCollectorManager.getBlockData(event.getBlockAgainst());

            if (blockData != null) {
                event.setCancelled(true);
                return;
            }
        }

        // ensure it is a chunk collector
        if (!item.getType().equals(v116 ? Material.END_PORTAL_FRAME : Material.valueOf("ENDER_PORTAL_FRAME"))) return;
        if (item.getItemMeta() == null || !item.getItemMeta().hasLore()) return;

        // check if it can't be placed in this world
        Block block = event.getBlockPlaced();
        if (ChunkCollectorManager.getDisabledWorlds().contains(block.getWorld().getName().toLowerCase())) {
            CANNOT_PLACE.msg(event.getPlayer());
            event.setCancelled(true);
            return;
        }

        // parse the tier from the lore
        int tier = 1;
        for (String s : Objects.requireNonNull(item.getItemMeta().getLore())) {
            if (s.contains("Tier:")) {
                tier = Integer.parseInt(ChatColor.stripColor(s.substring(s.indexOf(" ") + 1)));
            }
        }

        // register and send nice message
        ChunkCollectorManager.registerBlock(event.getPlayer().getUniqueId(), block, tier);
        PLACED.msg(event.getPlayer());
    }

    /**
     * Handles breaking a chunk collector in the world for creative mode players
     * Survival mode players are handled in the Packet Listener
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) return;

        // if you cant build here, don't break here
        if (!Hook.checkBuild(event.getPlayer(), event.getBlock())) return;

        BlockData blockData = ChunkCollectorManager.getBlockData(event.getBlock());
        if (blockData == null) return;

        event.setCancelled(true);
        event.getBlock().setType(Material.AIR);

        Location location = event.getBlock().getLocation();
        World world = location.getWorld();

        if (world == null) return;

        // closes open inventories of viewers
        List<Player> toClose = new ArrayList<>();
        for (UUID storageViewer : blockData.getStorageViewers()) {
            Player p = Bukkit.getPlayer(storageViewer);
            if (p != null)
                toClose.add(p);
        }
        toClose.forEach(HumanEntity::closeInventory);

        // drop the chunk collector
        ItemStack collector = COLLECTOR.clone();
        ItemMeta meta = collector.getItemMeta();
        Objects.requireNonNull(meta).setLore(getCollectorLore(blockData.getStoragePages()));
        collector.setItemMeta(meta);
        world.dropItemNaturally(location, collector);
        // drop all of the storage drops
        for (ItemStack itemStack : blockData.storageToItemStack()) {
            world.dropItemNaturally(location, itemStack);
        }

        // unregister this block
        ChunkCollectorManager.unregisterBlock(location);
    }

    /**
     * Handles the player opening the collector gui, or moving items from storage to inventory
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        // if you cant build here, don't allow access to this chunk collector
        if (!Hook.checkBuild(event.getPlayer(), event.getClickedBlock())) return;

        BlockData blockData = ChunkCollectorManager.getBlockData(event.getClickedBlock());
        if (blockData == null) return;

        event.setCancelled(true);

        Player player = event.getPlayer();

        // only allow one person to edit the crop hopper at a time
        if (!blockData.getStorageViewers().isEmpty()
                && !blockData.getStorageViewers().contains(player.getUniqueId())) {
            ALREADY_VIEWING.msg(event.getPlayer());
            return;
        }

        new CollectorGui(blockData, player.getUniqueId()).open(player);
        player.playSound(player.getLocation(),
                (v116 ? Sound.BLOCK_ENDER_CHEST_OPEN : (v112 ? Sound.valueOf("BLOCK_ENDERCHEST_OPEN") : Sound.valueOf("CHEST_OPEN"))),
                1.0f, 1.0f);
    }
}
