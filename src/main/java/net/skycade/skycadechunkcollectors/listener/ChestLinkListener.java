package net.skycade.skycadechunkcollectors.listener;

import net.skycade.skycadechunkcollectors.data.BlockData;
import net.skycade.skycadechunkcollectors.data.ChunkCollectorManager;
import net.skycade.skycadechunkcollectors.hook.Hook;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.skycade.skycadechunkcollectors.SkycadeChunkCollectorsPlugin.v116;
import static net.skycade.skycadechunkcollectors.SkycadeChunkCollectorsPlugin.v18;
import static net.skycade.skycadechunkcollectors.util.Messages.*;

public class ChestLinkListener implements Listener {

    private static Map<UUID, LinkSession> linkSessions = new HashMap<>();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!v18 && (event.getHand() == null || event.getHand().equals(EquipmentSlot.OFF_HAND))) return;

        Player player = event.getPlayer();
        if (!Hook.checkBuild(player, event.getClickedBlock())) return;
        if (!player.isSneaking() || !event.getAction().equals(Action.LEFT_CLICK_BLOCK)) return;
        if (event.getClickedBlock() == null) return;
        if (event.getItem() != null) return;

        if (!player.getGameMode().equals(GameMode.SURVIVAL)) event.setCancelled(true);

        Block block = event.getClickedBlock();
        if (block.getType() == (v116 ? Material.END_PORTAL_FRAME : Material.valueOf("ENDER_PORTAL_FRAME"))) {
            BlockData blockData = ChunkCollectorManager.getBlockData(block);

            if (blockData == null) return;

            linkSessions.put(player.getUniqueId(), new LinkSession(player.getUniqueId(), blockData));
            LINK_SESSION_START.msg(player);
        } else if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) {
            if (!linkSessions.containsKey(event.getPlayer().getUniqueId())) return;
            LinkSession linkSession = linkSessions.get(event.getPlayer().getUniqueId());

            Chunk collectorChunk = linkSession.collector.getLocation().getChunk();
            Chunk blockChunk = block.getChunk();

            if (Math.abs(collectorChunk.getX() - blockChunk.getX()) > 4 ||
                    Math.abs(collectorChunk.getZ() - blockChunk.getZ()) > 4) {
                CANNOT_LINK.msg(player);
                return;
            }

            if (linkSession.collector.getLinkedChests().size() >= 5) {
                LIMIT_EXCEEDED.msg(player);
                return;
            }

            linkSession.collector.addLinkedChest(block.getLocation());
            LINKED.msg(player);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (linkSessions.containsKey(player.getUniqueId())) {
            linkSessions.remove(player.getUniqueId());
            LINK_SESSION_PAUSE.msg(player);
        }
    }

    private static class LinkSession {
        private final UUID uuid;
        private BlockData collector;

        LinkSession(UUID uuid, BlockData collector) {
            this.uuid = uuid;
            this.collector = collector;
        }
    }
}
