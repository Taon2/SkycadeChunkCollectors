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
import net.skycade.skycadechunkcollectors.data.SimpleItem;
import net.skycade.skycadechunkcollectors.event.ItemAutoSaleEvent;
import net.skycade.skycadeshop.SkycadeShopPlugin;
import net.skycade.skycadeshop.shop.Shop;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
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
    private Shop shop = SkycadeShopPlugin.getInstance().getShop();

    @Override
    public void run() {
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

                    // loop through all chunk collectors for this chunk
                    for (BlockData blockData : data) {
                        Location location = blockData.getLocation();
                        Block block = location.getBlock();

                        // ignore this collector if it's not a chunk collector
                        if (!(block.getType() == (v116 ? Material.END_PORTAL_FRAME : Material.valueOf("ENDER_PORTAL_FRAME")))) continue;

                        // handle the items and then send to chests if possible
                        // remaining unhandled items are still stored in items for next iteration
                        List<SimpleItem> toSell = handleItems(blockData, items);
                        // sell whatever items should be sold
                        if (!toSell.isEmpty())
                            sellItems(blockData, toSell);
                        // send all possible items to chests
                        sendToChests(blockData);
                    }
                }
            }
        }
    }

    /**
     * A Chunk Collector handles the list of items, determining where they should go
     * @param blockData The Chunk Collector to handle the items
     * @param items The items to be handled
     * @return The list of items that should be automatically sold to the shop
     */
    private List<SimpleItem> handleItems(BlockData blockData, List<Entity> items) {
        List<SimpleItem> toSell = new ArrayList<>();

        // loop through list of item entities to handle
        for (Entity e : new ArrayList<>(items)) {
            if (!e.isValid() || e.isDead()) continue;
            Item item = (Item) e;
            ItemStack itemStack = item.getItemStack();

            // stops trail items from being picked up
            if (itemStack.hasItemMeta()
                    && itemStack.getItemMeta() != null
                    && itemStack.getItemMeta().hasDisplayName()
                    && itemStack.getItemMeta().getDisplayName().contains("Rain_ItId")) {
                // remove item from next iteration
                items.remove(e);
                continue;
            }

            // ignore this collector if it cannot pick up this item type
            if (!blockData.getTypes().contains(itemStack.getType())) continue;

            // determine what to do with this item here
            if (Bukkit.getPluginManager().isPluginEnabled("Factions") && itemStack.getType() == Material.TNT) {
                // if factions is enabled and the item is tnt, send it to the faction bank
                FPlayer fPlayer = FPlayers.getInstance().getByOfflinePlayer(Bukkit.getOfflinePlayer(blockData.getUuid()));

                if (fPlayer.hasFaction()) {
                    Faction faction = fPlayer.getFaction();
                    faction.setTNTBank(faction.getTNTBank() + itemStack.getAmount());
                } else {
                    // store the item if no faction
                    blockData.addToStorage(itemStack);
                }

                // remove the item from next iteration and world
                items.remove(e);
                e.remove();
            } else if (blockData.doAutosell()) {
                // add the items that should be sold
                boolean handled = false;
                for (SimpleItem i : toSell) {
                    if (i.getMaterial() == itemStack.getType()
                            && i.getData() == itemStack.getDurability()) {
                        i.setAmount(i.getAmount() + itemStack.getAmount());
                        handled = true;
                        break;
                    }
                }

                // add a new item, because it wasn't in the list yet
                if (!handled) {
                    SimpleItem simpleItem = new SimpleItem(itemStack.getType(), itemStack.getDurability(), itemStack.getAmount());
                    toSell.add(simpleItem);
                }

                // remove the item from next iteration and world
                items.remove(e);
                e.remove();
            } else {
                // store the item
                blockData.addToStorage(itemStack);
                // remove the item from next iteration and world
                items.remove(e);
                e.remove();
            }
        }

        return toSell;
    }

    /**
     * Sells a list of items to shop in one cycle, saving on lag from multiple calls
     * @param blockData The Collector to sell items from
     * @param toSell The list of items to sell to shop
     */
    private void sellItems(BlockData blockData, List<SimpleItem> toSell) {
        Block block = blockData.getLocation().getBlock();
        OfflinePlayer op = Bukkit.getOfflinePlayer(blockData.getUuid());

        // loop through the items to sell
        for (SimpleItem simpleItem : toSell) {
            // try to sell the item
            shop.get("item:" + simpleItem.getMaterial().name()).ifPresent(s -> {
                if (!s.isSellable()) return;

                double value = s.getUnitSellPrice() * simpleItem.getAmount();

                ItemAutoSaleEvent event =
                        new ItemAutoSaleEvent(block, simpleItem.getMaterial(), simpleItem.getData(), simpleItem.getAmount(), op, value);
                Bukkit.getPluginManager().callEvent(event);

                if (event.isCancelled())
                    return;

                AsyncScheduler.runTask(SkycadeChunkCollectorsPlugin.getInstance(), () -> {
                    econ.depositPlayer(op, event.getNewCost());
                });
            });
        }
    }

    /**
     * Empty a Collector into the Chests it is linked to
     * @param blockData The raw collector data
     */
    private void sendToChests(BlockData blockData) {
        // don't try to empty a collector that has no linked chests
        if (blockData.getLinkedChests().isEmpty()) return;
        // don't try to empty a collector when a player is viewing the gui - prevents dupes
        if (blockData.getStorageViewers().size() > 0) return;

        Collection<ItemStack> toSend = blockData.storageToItemStack();
        // loop through all linked chests
        for (Iterator<Location> i = blockData.getLinkedChests().iterator(); i.hasNext();) {
            Location linkedChestLocation = i.next();
            if (!linkedChestLocation.getChunk().isLoaded()) continue;

            Block linkedChestBlock = linkedChestLocation.getBlock();
            // remove if its not an actual chest
            if (linkedChestBlock.getType() != Material.CHEST && linkedChestBlock.getType() != Material.TRAPPED_CHEST) {
                i.remove();
                blockData.removeLinkedChest(linkedChestLocation);
                continue;
            }

            // don't try to empty a collector that has no items
            if (toSend.isEmpty()) return;

            Chest linkedChestState = (Chest) linkedChestBlock.getState();

            toSend = linkedChestState.getInventory().addItem(toSend.toArray(new ItemStack[]{})).values();
            if (toSend.isEmpty())
                break;
        }

        blockData.keepInStorage(new ArrayList<>(toSend));
    }
}
