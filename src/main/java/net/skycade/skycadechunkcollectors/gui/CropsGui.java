package net.skycade.skycadechunkcollectors.gui;

import net.md_5.bungee.api.ChatColor;
import net.skycade.SkycadeCore.guis.dynamicnew.DynamicGui;
import net.skycade.api.utility.ItemBuilder;
import net.skycade.skycadechunkcollectors.data.BlockData;
import net.skycade.skycadechunkcollectors.data.ChunkCollectorManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.skycade.skycadechunkcollectors.SkycadeChunkCollectorsPlugin.v18;

public class CropsGui extends DynamicGui {

    private static final ItemStack BACK = new ItemBuilder(Material.ARROW)
            .setDisplayName(ChatColor.GOLD + "Go Back")
            .build();
    private static final ItemStack NEXT = new ItemBuilder(Material.ARROW)
            .setDisplayName(ChatColor.GOLD + "Next")
            .build();

    public CropsGui(BlockData blockData, UUID uuid, int page) {
        super(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Crops", 6);
        blockData.addStorageViewer(uuid);

        setCloseInteraction((p, ev) -> blockData.removeStorageViewer(uuid));

        ChunkCollectorManager.getCrops().stream()
                .skip((page - 1) * 45)
                .limit(45)
                .forEach(material -> addItemInteraction(p -> {
                            ItemStack item = new ItemStack(material, 1);

                            ItemMeta meta = Bukkit.getItemFactory().getItemMeta(item.getType());

                            List<String> lore = new ArrayList<>();

                            // enchant and change lore if item is already picked up
                            if (blockData.containsType(material)) {
                                lore.add(ChatColor.GRAY + "Click to ignore this item.");
                                meta.addEnchant(Enchantment.DURABILITY, 10, true);
                                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                            } else {
                                lore.add(ChatColor.GRAY + "Click to pick up this item.");
                            }

                            meta.setLore(lore);
                            item.setItemMeta(meta);

                            return item;
                        },
                        (p, ev) -> {
                            ItemStack clicked = ev.getCurrentItem();
                            if (clicked == null) return;
                            Material m = clicked.getType();

                            if (blockData.containsType(m)) {
                                blockData.removeType(m);
                            } else {
                                blockData.addType(m);
                            }

                            p.playSound(p.getLocation(), v18 ? Sound.valueOf("ORB_PICKUP") : Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                            new CropsGui(blockData, uuid, page).open(p);
                        }));

        setItemInteraction(45, new ItemBuilder(BACK).build(),
                (p, ev) -> {
                    if (page > 1)
                        new CropsGui(blockData, uuid, page - 1).open(p);
                    else
                        new TypesGui(blockData, uuid).open(p);
                });

        if (ChunkCollectorManager.getCrops().size() > page * 45) {
            setItemInteraction(53, new ItemBuilder(NEXT).build(),
                    (p, ev) -> {
                        new CropsGui(blockData, uuid, page + 1).open(p);
                    });
        }
    }
}
