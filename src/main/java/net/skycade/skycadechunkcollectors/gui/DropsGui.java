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

public class DropsGui extends DynamicGui {

    private static final ItemStack BACK = new ItemBuilder(Material.ARROW)
            .setDisplayName(ChatColor.GOLD + "Go Back")
            .build();
    private static final ItemStack NEXT = new ItemBuilder(Material.ARROW)
            .setDisplayName(ChatColor.GOLD + "Next")
            .build();

    public DropsGui(BlockData blockData, UUID uuid, int page) {
        super(ChatColor.RED + "" + ChatColor.BOLD + "Drops", 6);
        blockData.addStorageViewer(uuid);

        setCloseInteraction((p, ev) -> blockData.removeStorageViewer(uuid));

        ChunkCollectorManager.getDrops().stream()
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

                            // if factions is enabled and the item is tnt, add this extra info
                            if (Bukkit.getPluginManager().isPluginEnabled("Factions") && item.getType() == Material.TNT) {
                                lore.add(ChatColor.RED + "" + ChatColor.BOLD + "TNT"
                                        + ChatColor.GRAY + " will be automatically sent to your faction's "
                                        + ChatColor.RED + "" + ChatColor.BOLD + "TNT Bank"
                                        + ChatColor.GRAY + ".");
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
                            new DropsGui(blockData, uuid, page).open(p);
                        }));

        setItemInteraction(45, new ItemBuilder(BACK).build(),
                (p, ev) -> {
                    if (page > 1)
                        new DropsGui(blockData, uuid, page - 1).open(p);
                    else
                        new TypesGui(blockData, uuid).open(p);
                });

        if (ChunkCollectorManager.getCrops().size() > page * 36) {
            setItemInteraction(53, new ItemBuilder(NEXT).build(),
                    (p, ev) -> {
                        new DropsGui(blockData, uuid, page + 1).open(p);
                    });
        }
    }
}
