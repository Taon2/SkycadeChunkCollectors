package net.skycade.skycadechunkcollectors;

import net.skycade.SkycadeCore.SkycadePlugin;
import net.skycade.skycadechunkcollectors.command.ChunkCollectorCommand;
import net.skycade.skycadechunkcollectors.data.ChunkCollectorManager;
import net.skycade.skycadechunkcollectors.hook.FactionsHook;
import net.skycade.skycadechunkcollectors.hook.Hook;
import net.skycade.skycadechunkcollectors.hook.SkyblockHook;
import net.skycade.skycadechunkcollectors.listener.ChunkCollectorListener;
import net.skycade.skycadechunkcollectors.runnable.ChunkCollectorRunnable;
import net.skycade.skycadechunkcollectors.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SkycadeChunkCollectorsPlugin extends SkycadePlugin {

    private static SkycadeChunkCollectorsPlugin instance;

    public SkycadeChunkCollectorsPlugin() {
        instance = this;
    }

    public static SkycadeChunkCollectorsPlugin getInstance() {
        return instance;
    }

    private BukkitTask chunkCollectorRunnable;

    public static boolean v18;
    public static boolean v112;
    public static boolean v116;

    @Override
    public void onEnable() {
        super.onEnable();

        v18 = Bukkit.getServer().getClass().getPackage().getName().contains("1_8");
        v112 = Bukkit.getServer().getClass().getPackage().getName().contains("1_12");
        v116 = Bukkit.getServer().getClass().getPackage().getName().contains("1_16");

        Messages.init();

        new ChunkCollectorCommand(this);

        loadConfig();

        Hook.registerHook(new SkyblockHook());
        Hook.registerHook(new FactionsHook());

        registerListeners(new ChunkCollectorListener());

        chunkCollectorRunnable = new ChunkCollectorRunnable().runTaskTimer(this, 5L, 50L);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        HandlerList.unregisterAll(this);
        try {
            if (chunkCollectorRunnable != null) chunkCollectorRunnable.cancel();
        } catch (Exception ignored) {}

        // save the chunk collectors to file
        ChunkCollectorManager.persist();
    }

    /**
     * Prepare a default config and load all saved values
     */
    private void loadConfig() {
        File file = new File(getDataFolder(), "collectors.json");
        if (!file.exists())
            saveResource("collectors.json", false);

        Map<String, Object> def = new HashMap<>();

        def.put("autosell-enabled", true);

        def.put("disabled-worlds", Arrays.asList("mining", "timemining"));

        YamlConfiguration drops = new YamlConfiguration();
        def.put("drops", drops);

        YamlConfiguration crops = new YamlConfiguration();
        def.put("crops", crops);

        YamlConfiguration ores = new YamlConfiguration();
        def.put("ores", ores);

        setConfigDefaults(def);
        loadDefaultConfig();

        // load the config and individual chunk collectors
        new BukkitRunnable() {
            @Override
            public void run() {
                ChunkCollectorManager.load();
            }
        }.runTaskLater(this, 1L);
    }
}
