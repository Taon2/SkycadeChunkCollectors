package net.skycade.skycadechunkcollectors.gui;

import net.md_5.bungee.api.ChatColor;
import net.skycade.SkycadeCore.guis.dynamicnew.DynamicGui;
import net.skycade.api.utility.ItemBuilder;
import net.skycade.skycadechunkcollectors.data.BlockData;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.UUID;

import static net.skycade.skycadechunkcollectors.SkycadeChunkCollectorsPlugin.v116;

public class TypesGui extends DynamicGui {

    private static final ItemStack CLEAR = new ItemBuilder(Material.BARRIER)
            .setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Clear All")
            .build();

    private static final ItemStack BACK = new ItemBuilder(Material.ARROW)
            .setDisplayName(ChatColor.GOLD + "Go Back")
            .build();

    private static final ItemStack DROPS = new ItemBuilder(v116 ? Material.GUNPOWDER : Material.valueOf("SULPHUR"))
            .setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Drops")
            .addToLore(ChatColor.GRAY + "Click to choose which Mob Drops are picked up.")
            .build();

    private static final ItemStack CROPS = new ItemBuilder(Material.WHEAT)
            .setDisplayName(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Crops")
            .addToLore(ChatColor.GRAY + "Click to choose which Crops are picked up.")
            .build();

    private static final ItemStack ORES = new ItemBuilder(Material.DIAMOND_ORE)
            .setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Ores")
            .addToLore(ChatColor.GRAY + "Click to choose which Ores are picked up.")
            .build();

    public TypesGui(BlockData blockData, UUID uuid) {
        super(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Drop Types", 3);
        blockData.addStorageViewer(uuid);

        setCloseInteraction((p, ev) -> blockData.removeStorageViewer(uuid));

        setItemInteraction(11, new ItemBuilder(DROPS).build(),
                (p, ev) -> {
                    p.playSound(p.getLocation(), (v116 ? Sound.BLOCK_NOTE_BLOCK_HAT : Sound.valueOf("NOTE_STICKS")), 1.0f, 1.0f);
                    new DropsGui(blockData, uuid, 1).open(p);
                });

        setItemInteraction(13, new ItemBuilder(CROPS).build(),
                (p, ev) -> {
                    p.playSound(p.getLocation(), (v116 ? Sound.BLOCK_NOTE_BLOCK_HAT : Sound.valueOf("NOTE_STICKS")), 1.0f, 1.0f);
                    new CropsGui(blockData, uuid, 1).open(p);
                });

        setItemInteraction(15, new ItemBuilder(ORES).build(),
                (p, ev) -> {
                    p.playSound(p.getLocation(), (v116 ? Sound.BLOCK_NOTE_BLOCK_HAT : Sound.valueOf("NOTE_STICKS")), 1.0f, 1.0f);
                    new OresGui(blockData, uuid, 1).open(p);
                });

        setItemInteraction(18, new ItemBuilder(BACK).build(),
                (p, ev) -> {
                    new CollectorGui(blockData, uuid).open(p);
                });

        setItemInteraction(26, new ItemBuilder(CLEAR).build(),
                (p, ev) -> {
                    for (Material type : new ArrayList<>(blockData.getTypes())) {
                        blockData.removeType(type);
                    }
                });
    }
}
