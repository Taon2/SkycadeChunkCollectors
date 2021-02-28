package net.skycade.skycadechunkcollectors.runnable;

import com.google.common.collect.TreeMultimap;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import net.milkbowl.vault.economy.Economy;
import net.skycade.SkycadeCore.utility.AsyncScheduler;
import net.skycade.skycadechunkcollectors.SkycadeChunkCollectorsPlugin;
import net.skycade.skycadechunkcollectors.data.BlockData;
import net.skycade.skycadechunkcollectors.data.ChunkLocation;
import net.skycade.skycadeshop.SkycadeShopPlugin;
import net.skycade.skycadeshop.event.ItemAutoSaleEvent;
import net.skycade.skycadeshop.shop.Shop;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

import static net.skycade.skycadechunkcollectors.SkycadeChunkCollectorsPlugin.v116;
import static net.skycade.skycadechunkcollectors.data.ChunkCollectorManager.getAllByChunk;

public class ChunkCollectorRunnable extends BukkitRunnable {

    private Economy econ = SkycadeShopPlugin.getInstance().getEconomy();

    @Override
    public void run() {
        Shop shop = SkycadeShopPlugin.getInstance().getShop();

        Map<String, TreeMultimap<ChunkLocation, BlockData>> all = getAllByChunk();

        // loop list of worlds with hoppers in them
        for (Map.Entry<String, TreeMultimap<ChunkLocation, BlockData>> wEntry : all.entrySet()) {
            // loop through list of all hoppers in world
            for (Map.Entry<ChunkLocation, Collection<BlockData>> entry : wEntry.getValue().asMap().entrySet()) {

                Collection<BlockData> data = entry.getValue();
                if (data.isEmpty()) continue;
                ChunkLocation loc = entry.getKey();

                World world = Bukkit.getWorld(loc.getWorld());
                if (world == null) continue;

                int x = loc.getX();
                int z = loc.getZ();

                // only run if chunk is loaded
                if (world.isChunkLoaded(x, z)) {
                    List<Entity> items = Arrays.stream(world.getChunkAt(x, z).getEntities())
                            .filter(Objects::nonNull)
                            .filter(e -> e.getType().equals(EntityType.DROPPED_ITEM)).collect(Collectors.toList());

                    // loop through all dropped items in this chunk
                    for (Entity e : items) {
                        if (!e.isValid() || e.isDead()) continue;
                        Item item = (Item) e;
                        ItemStack itemStack = item.getItemStack();

                        // stops trail items from being picked up
                        if (itemStack.hasItemMeta()
                                && itemStack.getItemMeta() != null
                                && itemStack.getItemMeta().hasDisplayName()
                                && itemStack.getItemMeta().getDisplayName().contains("Rain_ItId")) continue;

                        // list of material types that should be removed after the hoppers pick up what they can
                        List<Material> toRemove = new ArrayList<>();
                        // fill the collectors of this chunk with items
                        for (BlockData blockData : data) {
                            Location location = blockData.getLocation();
                            Block block = location.getBlock();

                            // ignore this collector if it's not a chunk collector
                            if (!(block.getType() == (v116 ? Material.END_PORTAL_FRAME : Material.valueOf("ENDER_PORTAL_FRAME")))) continue;
                            // ignore this collector if it cannot pick up this item type
                            if (!blockData.getTypes().contains(itemStack.getType())) continue;

                            // determine what to do with this item here
                            if (Bukkit.getPluginManager().isPluginEnabled("Factions") && itemStack.getType() == Material.TNT) {
                                // if factions is enabled and the item is tnt, send it to the faction bank
                                FPlayer fPlayer = FPlayers.getInstance().getByOfflinePlayer(Bukkit.getOfflinePlayer(blockData.getUuid()));

                                if (fPlayer.hasFaction()) {
                                    Faction faction = fPlayer.getFaction();
                                    faction.setTNTBank(faction.getTNTBank() + itemStack.getAmount());
                                }
                            } else if (blockData.doAutosell()) {
                                // sell the item
                                OfflinePlayer op = Bukkit.getOfflinePlayer(blockData.getUuid());

                                // try to sell the item
                                shop.get("item:" + itemStack.getType().name()).ifPresent(s -> {
                                    if (!s.isSellable()) return;

                                    double value = s.getUnitSellPrice() * itemStack.getAmount();

                                    ItemAutoSaleEvent event = new ItemAutoSaleEvent(block, itemStack, op, value);
                                    Bukkit.getPluginManager().callEvent(event);

                                    if (event.isCancelled())
                                        return;

                                    AsyncScheduler.runTask(SkycadeChunkCollectorsPlugin.getInstance(), () -> {
                                        econ.depositPlayer(op, event.getNewCost());
                                    });
                                });
                            } else {
                                // store the item
                                blockData.addToStorage(itemStack);
                            }

                            // add the item type to be removed
                            toRemove.add(itemStack.getType());

                            // this itemstack has been handled if it gets this far, no need to loop through more collectors
                            break;
                        }

                        // right now, we want to remove the item stack whether it was collected (no dupes)
                        // or if it isn't collected, to prevent extra items creating lag
                        // obviously, only remove item types that were attempted to be picked up, but failed to do so
                        // TL;DR always remove the item
                        if (toRemove.contains(itemStack.getType()))
                            e.remove();
                    }
                }
            }
        }
    }
}
