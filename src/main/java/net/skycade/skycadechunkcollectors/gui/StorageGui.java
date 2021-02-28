package net.skycade.skycadechunkcollectors.gui;

import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.skycade.SkycadeCore.guis.dynamicnew.DynamicGui;
import net.skycade.api.utility.ItemBuilder;
import net.skycade.skycadechunkcollectors.SkycadeChunkCollectorsPlugin;
import net.skycade.skycadechunkcollectors.data.BlockData;
import net.skycade.skycadeshop.SkycadeShopPlugin;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

import static net.skycade.skycadechunkcollectors.util.Messages.NOT_ENOUGH_MONEY;
import static net.skycade.skycadechunkcollectors.util.Messages.SUCCESS;

public class StorageGui extends DynamicGui {

    private Economy econ = SkycadeShopPlugin.getInstance().getEconomy();
    private static double price = SkycadeChunkCollectorsPlugin.getInstance().getConfig().getDouble("page-price");

    private static final ItemStack UNLOCK_PAGE = new ItemBuilder(Material.CHEST)
            .setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Unlock Page")
            .addToLore(ChatColor.GOLD + "" + ChatColor.BOLD + "Price: " + ChatColor.GRAY + "$" + price)
            .addToLore(ChatColor.GRAY + "Purchase another page of storage")
            .addToLore(ChatColor.GRAY + "for this Chunk Collector.")
            .build();
    private static final ItemStack BACK = new ItemBuilder(Material.ARROW)
            .setDisplayName(ChatColor.GOLD + "Go Back")
            .build();
    private static final ItemStack NEXT = new ItemBuilder(Material.ARROW)
            .setDisplayName(ChatColor.GOLD + "Next")
            .build();


    public StorageGui(BlockData blockData, UUID uuid, int page) {
        super(ChatColor.GOLD + "" + ChatColor.BOLD + "Storage - Page " + page, 6);
        blockData.addStorageViewer(uuid);

        setCloseInteraction((p, ev) -> blockData.removeStorageViewer(uuid));

        blockData.storageToItemStack().stream()
                .skip((page - 1) * 45)
                .limit(45)
                .forEach(item -> addItemInteraction(p -> item,
                        (p, ev) -> {
                            // handle player inventory click
                            if (ev.getClickedInventory() != null
                                    && ev.getClickedInventory().getType() == InventoryType.PLAYER) {

                                // allow players to place in their inventory
                                if (ev.getCursor() != null && ev.getCursor().getType() != Material.AIR)
                                    ev.setCancelled(false);

                                return;
                            }

                            // empty cursor means they are only removing items, and are not trying to add items
                            if (ev.getCursor() == null || ev.getCursor().getType() == Material.AIR) {
                                ev.setCancelled(false);
                                blockData.removeFromStorage(ev.getCurrentItem());
                            }
                        }));

        setItemInteraction(49, new ItemBuilder(UNLOCK_PAGE).build(),
                (p, ev) -> {
                    if (econ.withdrawPlayer(p, price).type == EconomyResponse.ResponseType.FAILURE) {
                        NOT_ENOUGH_MONEY.msg(p);
                    } else {
                        blockData.addStoragePage();
                        SUCCESS.msg(p);
                        new StorageGui(blockData, uuid, page).open(p);
                    }
                });

        setItemInteraction(45, new ItemBuilder(BACK).build(),
                (p, ev) -> {
                    if (page > 1)
                        new StorageGui(blockData, uuid, page - 1).open(p);
                    else
                        new CollectorGui(blockData, uuid).open(p);
                });

        if (blockData.getStoragePages() > page) {
            setItemInteraction(53, new ItemBuilder(NEXT).build(),
                    (p, ev) -> {
                        new StorageGui(blockData, uuid, page + 1).open(p);
                    });
        }
    }
}
