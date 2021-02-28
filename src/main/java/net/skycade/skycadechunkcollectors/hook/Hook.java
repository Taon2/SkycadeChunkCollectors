package net.skycade.skycadechunkcollectors.hook;

import net.skycade.skycadechunkcollectors.SkycadeChunkCollectorsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;

public abstract class Hook implements Listener {

    public static Set<Hook> hooks = new HashSet<>();

    public static void registerHook(Hook hook) {
        if (Bukkit.getPluginManager().isPluginEnabled(hook.name)) {
            hooks.add(hook);
            Bukkit.getPluginManager().registerEvents(hook, SkycadeChunkCollectorsPlugin.getInstance());
        }
    }

    public static boolean checkBuild(Player player, Block block) {
        for (Hook hook : hooks) {
            if (Bukkit.getPluginManager().isPluginEnabled(hook.name) && !hook.canBuild(player, block))
                return false;
        }

        return true;
    }

    private final String name;

    public Hook(String name) {
        this.name = name;
    }

    public abstract boolean canBuild(Player player, Block block);
}
