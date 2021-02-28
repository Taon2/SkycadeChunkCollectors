package net.skycade.skycadechunkcollectors.command;

import net.md_5.bungee.api.ChatColor;
import net.skycade.SkycadeCore.utility.ItemBuilder;
import net.skycade.SkycadeCore.utility.command.SkycadeCommand;
import net.skycade.SkycadeCore.utility.command.addons.Permissible;
import net.skycade.SkycadeCore.utility.command.addons.SubCommand;
import net.skycade.skycadechunkcollectors.SkycadeChunkCollectorsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static net.skycade.SkycadeCore.Localization.Global.INVALID_INTEGER;
import static net.skycade.SkycadeCore.Localization.Global.PLAYER_NOT_FOUND;
import static net.skycade.skycadechunkcollectors.SkycadeChunkCollectorsPlugin.v116;
import static net.skycade.skycadechunkcollectors.util.Messages.SUCCESS;
import static net.skycade.skycadechunkcollectors.util.Messages.USAGE;

public class ChunkCollectorCommand extends SkycadeCommand {
    public static final ItemStack COLLECTOR = new ItemBuilder(v116 ? Material.END_PORTAL_FRAME : Material.valueOf("ENDER_PORTAL_FRAME"))
            .setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Chunk Collector")
            .build();

    public static List<String> getCollectorLore(int tier) {
        return Arrays.asList(
                ChatColor.AQUA + "" + ChatColor.BOLD + "Tier: " + ChatColor.WHITE + tier,
                ChatColor.GRAY + "Place in a chunk and configure to",
                ChatColor.GRAY + "automatically store or sell item drops!"
        );
    }

    private final SkycadeChunkCollectorsPlugin plugin;

    public ChunkCollectorCommand(SkycadeChunkCollectorsPlugin plugin) {
        super("collector");
        this.plugin = plugin;

        addSubCommands(new Give());
        addSubCommands(new Reload());
    }

    @Override
    public void onCommand(CommandSender sender, String[] strings) {
        USAGE.msg(sender);
    }

    @SubCommand
    @Permissible("skycade.chunkcollectors.give")
    public class Give extends SkycadeCommand {

        Give() {
            super("give");
        }

        @Override
        public void onCommand(CommandSender sender, String[] args) {
            if (args.length < 2) {
                USAGE.msg(sender);
                return;
            }

            // check player
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                PLAYER_NOT_FOUND.msg(sender);
                return;
            }

            // check amount
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                INVALID_INTEGER.msg(sender);
                return;
            }

            if (amount < 1) {
                INVALID_INTEGER.msg(sender);
                return;
            }

            ItemStack collector = COLLECTOR.clone();
            ItemMeta meta = collector.getItemMeta();
            Objects.requireNonNull(meta).setLore(getCollectorLore(1));
            collector.setItemMeta(meta);

            target.getInventory().addItem(collector);
            target.updateInventory();

            SUCCESS.msg(sender);
        }
    }

    @Permissible("skycade.chunkcollectors.reload")
    private class Reload extends SkycadeCommand {
        Reload() {
            super("reload");
        }

        @Override
        public void onCommand(CommandSender sender, String[] args) {
            plugin.onDisable();
            plugin.onEnable();

            sender.sendMessage("Done!");
        }
    }
}
