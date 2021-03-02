package net.skycade.skycadechunkcollectors.listener.packets;

import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import net.skycade.skycadechunkcollectors.data.BlockData;
import net.skycade.skycadechunkcollectors.data.ChunkCollectorManager;
import net.skycade.skycadechunkcollectors.hook.Hook;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static com.comphenix.protocol.PacketType.Play.Client.BLOCK_DIG;
import static net.skycade.skycadechunkcollectors.SkycadeChunkCollectorsPlugin.v116;
import static net.skycade.skycadechunkcollectors.command.ChunkCollectorCommand.COLLECTOR;
import static net.skycade.skycadechunkcollectors.command.ChunkCollectorCommand.getCollectorLore;

public class EndPortalFramePacketListener implements PacketListener {

    private final JavaPlugin plugin;

    private static Map<UUID, EndPortalFramePacketListener.Status> map = new HashMap<>();

    public EndPortalFramePacketListener(JavaPlugin plugin) {
        this.plugin = plugin;

        new BukkitRunnable() {
            @Override
            public synchronized void run() {
                Iterator<Map.Entry<UUID, EndPortalFramePacketListener.Status>> i = map.entrySet().iterator();
                long now = System.currentTimeMillis();
                while (i.hasNext()) {
                    Map.Entry<UUID, EndPortalFramePacketListener.Status> entry = i.next();
                    if (!entry.getValue().getBlock().getType().equals(v116 ? Material.END_PORTAL_FRAME : Material.valueOf("ENDER_PORTAL_FRAME"))
                            || Bukkit.getPlayer(entry.getKey()) == null) {
                        i.remove();
                        continue;
                    }

                    EndPortalFramePacketListener.Status status = entry.getValue();
                    Block block = status.getBlock();
                    if (now - status.getBegin() >= 2000) {
                        Player player = Bukkit.getPlayer(status.getUuid());
                        if (player == null) return;

                        BlockBreakEvent event = new BlockBreakEvent(block, player);
                        i.remove();

                        Bukkit.getPluginManager().callEvent(event);
                        if (event.isCancelled()) return;

                        breakCollector(event.getPlayer(), event.getBlock());
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    /**
     * Handles breaking a chunk collector in the world
     */
    private void breakCollector(Player player, Block block) {
        // if you cant build here, don't break here
        if (!Hook.checkBuild(player, block)) return;

        BlockData blockData = ChunkCollectorManager.getBlockData(block);
        if (blockData == null) return;

        block.setType(Material.AIR);

        Location location = block.getLocation();
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

    @Override
    public void onPacketSending(PacketEvent packetEvent) {}

    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {
        if (!packetEvent.getPacketType().equals(BLOCK_DIG)) return;
        Player player = packetEvent.getPlayer();
        if (!player.getGameMode().equals(GameMode.SURVIVAL)) return;
        ItemStack item = player.getInventory().getItemInHand();
        if (!item.getType().equals(Material.DIAMOND_PICKAXE)) return;
        EnumWrappers.PlayerDigType digType = packetEvent.getPacket().getPlayerDigTypes().read(0);

        BlockPosition blockPos = packetEvent.getPacket().getBlockPositionModifier().read(0);
        Block block = blockPos.toLocation(packetEvent.getPlayer().getWorld()).getBlock();

        if (digType.equals(EnumWrappers.PlayerDigType.ABORT_DESTROY_BLOCK)) {
            map.remove(player.getUniqueId());
        }

        if (!block.getType().equals(v116 ? Material.END_PORTAL_FRAME : Material.valueOf("ENDER_PORTAL_FRAME"))
                || !player.getGameMode().equals(GameMode.SURVIVAL)
                || !digType.equals(EnumWrappers.PlayerDigType.START_DESTROY_BLOCK)
                || ChunkCollectorManager.getBlockData(block) == null)
            return;

        packetEvent.setCancelled(true);
        map.put(player.getUniqueId(), new EndPortalFramePacketListener.Status(player.getUniqueId(), block));

    }

    @Override
    public ListeningWhitelist getSendingWhitelist() {
        return ListeningWhitelist.EMPTY_WHITELIST;
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder().types(BLOCK_DIG).build();
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }

    public static class Status {
        private Long begin;
        private UUID uuid;
        private Block block;

        public Status(UUID uuid, Block block) {
            this.uuid = uuid;
            this.block = block;
            begin = System.currentTimeMillis();
        }

        public Long getBegin() {
            return begin;
        }

        public UUID getUuid() {
            return uuid;
        }

        public Block getBlock() {
            return block;
        }
    }
}
