package net.skycade.skycadechunkcollectors.gui;

import net.md_5.bungee.api.ChatColor;
import net.skycade.SkycadeCore.guis.dynamicnew.DynamicGui;
import net.skycade.api.utility.ItemBuilder;
import net.skycade.skycadechunkcollectors.data.BlockData;
import net.skycade.skycadechunkcollectors.data.ChunkCollectorManager;
import net.skycade.skycadechunkcollectors.data.SimpleItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

import static net.skycade.skycadechunkcollectors.SkycadeChunkCollectorsPlugin.v116;
import static net.skycade.skycadechunkcollectors.SkycadeChunkCollectorsPlugin.v18;

public class CollectorGui extends DynamicGui {

    private static final ItemStack STORAGE = new ItemBuilder(Material.CHEST)
            .setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Storage")
            .build();

    private static final ItemStack TYPES = new ItemBuilder(v116 ? Material.GUNPOWDER : Material.valueOf("SULPHUR"))
            .setDisplayName(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Drop Types")
            .build();

    private static final ItemStack AUTOSELL = new ItemBuilder(Material.PAPER)
            .build();

    public CollectorGui(BlockData blockData, UUID uuid) {
        super(ChatColor.AQUA + "" + ChatColor.BOLD + "Chunk Collector", 3);
        blockData.addStorageViewer(uuid);

        setCloseInteraction((p, ev) -> blockData.removeStorageViewer(uuid));

        ItemBuilder storage = new ItemBuilder(STORAGE);
        OfflinePlayer op = Bukkit.getOfflinePlayer(blockData.getUuid());
        storage.addToLore(ChatColor.GOLD + "" + ChatColor.BOLD + "Owner: " + ChatColor.GRAY + op.getName());
        storage.addToLore(ChatColor.GOLD + "" + ChatColor.BOLD + "Pages: " + ChatColor.GRAY + blockData.getStoragePages());

        if (blockData.getStorage().size() > 0)
            storage.addToLore(ChatColor.GOLD + "" + ChatColor.BOLD + "Contents:");

        for (SimpleItem item : blockData.getStorage()) {
            if (item.getData() == 0) {
                storage.addToLore(ChatColor.GRAY + "" + item.getMaterial().name() + " x " + item.getAmount());
            } else {
                storage.addToLore(ChatColor.GRAY + "" + item.getMaterial().name() + ":" + item.getData() + " x " + item.getAmount());
            }
        }

        setItemInteraction(11, storage.build(),
                (p, ev) -> {
                    p.playSound(p.getLocation(), (v116 ? Sound.BLOCK_CHEST_OPEN : Sound.valueOf("CHEST_OPEN")), 1.0f, 1.0f);
                    new StorageGui(blockData, uuid, 1).open(p);
                });

        ItemBuilder types = new ItemBuilder(TYPES);
        if (blockData.getTypes().size() > 0)
            types.addToLore(ChatColor.GOLD + "" + ChatColor.BOLD + "Collecting:");

        for (Material type : blockData.getTypes()) {
            types.addToLore(ChatColor.GRAY + "" + type);
        }

        setItemInteraction(13, types.build(),
                (p, ev) -> {
                    p.playSound(p.getLocation(), (v116 ? Sound.BLOCK_NOTE_BLOCK_HAT : Sound.valueOf("NOTE_STICKS")), 1.0f, 1.0f);
                    new TypesGui(blockData, uuid).open(p);
                });

        ItemBuilder autosell;
        if (!ChunkCollectorManager.isAutosellEnabled()) {
            autosell = new ItemBuilder(AUTOSELL)
                    .setDisplayName(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Sending to " + ChatColor.GOLD + "" + ChatColor.BOLD + "Storage");
        } else if (blockData.doAutosell()) {
            autosell = new ItemBuilder(AUTOSELL)
                    .setDisplayName(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Sending to " + ChatColor.RED + "" + ChatColor.BOLD + "Autosell")
                    .addToLore(ChatColor.GRAY + "Click to send all items to " + ChatColor.GOLD + "" + ChatColor.BOLD + "Storage");
        } else {
            autosell = new ItemBuilder(AUTOSELL)
                    .setDisplayName(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Sending to " + ChatColor.GOLD + "" + ChatColor.BOLD + "Storage")
                    .addToLore(ChatColor.GRAY + "Click to send all items to " + ChatColor.RED + "" + ChatColor.BOLD + "Autosell");
        }

        setItemInteraction(15, autosell.build(),
                (p, ev) -> {
                    if (ChunkCollectorManager.isAutosellEnabled()) {
                        blockData.toggleAutosell();
                        p.playSound(p.getLocation(), v18 ? Sound.valueOf("ORB_PICKUP") : Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                        new CollectorGui(blockData, uuid).open(p);
                    }
                });
    }
}
