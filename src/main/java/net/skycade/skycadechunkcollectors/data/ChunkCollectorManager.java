package net.skycade.skycadechunkcollectors.data;

import com.google.common.collect.TreeMultimap;
import com.google.gson.*;
import net.skycade.skycadechunkcollectors.SkycadeChunkCollectorsPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static net.skycade.skycadechunkcollectors.SkycadeChunkCollectorsPlugin.v116;

public class ChunkCollectorManager {
    private static Map<Location, BlockData> map = new TreeMap<>((o1, o2) -> {
        int a = Double.compare(o1.getY(), o2.getY());
        if (a == 0) return Integer.compare(o1.hashCode(), o2.hashCode());
        return a;
    });

    public static Collection<BlockData> getAllBlockData() {
        return map.values();
    }

    private static Map<String, TreeMultimap<ChunkLocation, BlockData>> byChunk = new HashMap<>();

    public static TreeMultimap<ChunkLocation, BlockData> getByChunkMap(String world) {
        return byChunk.computeIfAbsent(world, w -> TreeMultimap.create());
    }

    public static Map<String, TreeMultimap<ChunkLocation, BlockData>> getAllByChunk() {
        return byChunk;
    }

    private static boolean autosellEnabled;

    private static Set<String> disabledWorlds = new HashSet<>();
    private static LinkedHashSet<Material> crops = new LinkedHashSet<>();
    private static LinkedHashSet<Material> drops = new LinkedHashSet<>();
    private static LinkedHashSet<Material> ores = new LinkedHashSet<>();

    public static Set<String> getDisabledWorlds() {
        return disabledWorlds;
    }

    public static Set<Material> getCrops() {
        return crops;
    }

    public static Set<Material> getDrops() {
        return drops;
    }

    public static Set<Material> getOres() {
        return ores;
    }

    /**
     * Register a location as a chunk collector and store it in the map
     * @param block The block to register as a chunk collector
     */
    public static void registerBlock(UUID uuid, Block block, int tier) {
        BlockData value = new BlockData(uuid, block.getLocation(), tier);
        BlockData put = map.put(block.getLocation(), value);
        Chunk chunk = block.getChunk();

        String world = block.getWorld().getName();
        if (put != null) {
            Chunk oldChunk = put.getLocation().getChunk();
            getByChunkMap(world).get(new ChunkLocation(oldChunk.getX(), oldChunk.getZ(), oldChunk.getWorld().getName())).remove(put);
        }

        ChunkLocation cl = new ChunkLocation(chunk.getX(), chunk.getZ(), chunk.getWorld().getName());
        getByChunkMap(world).put(cl, value);
        getByChunkMap(world).entries().removeIf(u -> !map.containsValue(u.getValue()));

        persist();
    }

    /**
     * Unregister a location as a chunk collector and remove it from the map
     * @param location The location to unregister as a chunk collector
     */
    public static void unregisterBlock(Location location) {
        String world = Objects.requireNonNull(location.getWorld()).getName();
        map.remove(location);
        getByChunkMap(world).entries().removeIf(u -> !map.containsValue(u.getValue()));

        persist();
    }

    /**
     * Get the collector data of a specified block
     * @param block The block to get the data from
     * @return The chunk collector data for this block. Null if not a chunk collector.
     */
    public static BlockData getBlockData(Block block) {
        // chunk collectors can only be end portal frames
        if (block.getType() != (v116 ? Material.END_PORTAL_FRAME : Material.valueOf("ENDER_PORTAL_FRAME"))) return null;

        // get the data
        for (BlockData blockData : getByChunkMap(block.getWorld().getName()).get(
                new ChunkLocation(block.getChunk().getX(), block.getChunk().getZ(), block.getWorld().getName()))) {
            if (blockData.getLocation().toVector().equals(block.getLocation().toVector())) {
                return blockData;
            }
        }
        return null;
    }

    /**
     * Load all config values and all individual chunk collectors
     */
    public static void load() {
        FileConfiguration config = SkycadeChunkCollectorsPlugin.getInstance().getConfig();

        autosellEnabled = config.getBoolean("autosell-enabled");

        // load disabled worlds list
        if (config.contains("disabled-worlds"))
            disabledWorlds = config.getStringList("disabled-worlds")
                    .stream().map(String::toLowerCase).collect(Collectors.toSet());
        else
            disabledWorlds = new HashSet<>();

        // load crops list
        if (config.contains("crops")) {
            for (String s : config.getStringList("crops")) {
                Material material;
                try {
                    material = Material.valueOf(s.toUpperCase());
                } catch (Exception e) {
                    material = null;
                }

                // ignore the block forms of these if its not 1.16
                if (!v116 && (material == Material.CARROT || material == Material.POTATO))
                    continue;

                if (material != null && material != Material.AIR) {
                    crops.add(material);
                }
            }
        }

        // load mob drops list
        if (config.contains("drops")) {
            for (String s : config.getStringList("drops")) {
                Material material;
                try {
                    material = Material.valueOf(s.toUpperCase());
                } catch (Exception e) {
                    material = null;
                }

                if (material != null && material != Material.AIR) {
                    drops.add(material);
                }
            }
        }

        // load ores list
        if (config.contains("ores")) {
            for (String s : config.getStringList("ores")) {
                Material material;
                try {
                    material = Material.valueOf(s.toUpperCase());
                } catch (Exception e) {
                    material = null;
                }

                if (material != null && material != Material.AIR) {
                    ores.add(material);
                }
            }
        }

        // load the individual placed collectors
        File file = new File(SkycadeChunkCollectorsPlugin.getInstance().getDataFolder(), "collectors.json");
        try {
            JsonElement parse = new JsonParser().parse(new FileReader(file));
            JsonObject json = parse.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                JsonObject blockData = entry.getValue().getAsJsonObject();
                // load location
                String world = blockData.get("world").getAsString();
                World w = Bukkit.getWorld(world);
                if (w == null) continue;
                int x = blockData.get("x").getAsInt();
                int y = blockData.get("y").getAsInt();
                int z = blockData.get("z").getAsInt();
                Location loc = new Location(w, x, y, z);

                // load owner
                UUID uuid = UUID.fromString(blockData.get("uuid").getAsString());

                // load configured types
                List<Material> types = new ArrayList<>();
                for (JsonElement jsonElement : blockData.getAsJsonArray("types")) {
                    types.add(Material.valueOf(jsonElement.getAsString()));
                }

                // load storage
                List<SimpleItem> storage = new ArrayList<>();
                JsonElement storageData = blockData.get("storage");
                if (storageData != null  && storageData.isJsonArray()) {
                    for (JsonElement stored : storageData.getAsJsonArray()) {
                        JsonObject storedItem = stored.getAsJsonObject();

                        SimpleItem simpleItem = new SimpleItem(
                                Material.valueOf(storedItem.get("material").getAsString()),
                                storedItem.get("data").getAsShort(),
                                storedItem.get("amount").getAsInt());
                        storage.add(simpleItem);
                    }
                }

                // load unlocked pages
                int storagepages = blockData.get("storagePages").getAsInt();

                // load autosell toggle
                boolean doAutosell = blockData.get("autosell").getAsBoolean();

                BlockData value = new BlockData(uuid, loc, types, storage, storagepages, doAutosell);

                map.put(loc, value);

                String worldName = loc.getWorld().getName();
                getByChunkMap(worldName).put(new ChunkLocation(loc.getBlockX() >> 4, loc.getBlockZ() >> 4, worldName), value);
            }

            persist();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Save or update a chunk collector in the file
     */
    public static synchronized void persist() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Map<String, Object> jsonMap = new HashMap<>();
        for (Map.Entry<Location, BlockData> entry : map.entrySet()) {
            Location loc = entry.getKey();
            BlockData data = entry.getValue();

            Map<String, Object> dataMap = new HashMap<>();
            // save location
            dataMap.put("world", loc.getWorld().getName());
            dataMap.put("x", loc.getBlockX());
            dataMap.put("y", loc.getBlockY());
            dataMap.put("z", loc.getBlockZ());

            // save owner
            dataMap.put("uuid", data.getUuid().toString());

            // save configured types
            dataMap.put("types", data.getTypes());

            // save storage
            List<Map<String, Object>> storage = new ArrayList<>();
            for (SimpleItem item : data.getStorage()) {
                Map<String, Object> storageMap = new HashMap<>();
                storageMap.put("material", item.getMaterial().name().toUpperCase());
                storageMap.put("data", item.getData());
                storageMap.put("amount", item.getAmount());
                storage.add(storageMap);
            }
            dataMap.put("storage", storage);

            // save unlocked pages
            dataMap.put("storagePages", data.getStoragePages());

            // save autosell toggle
            dataMap.put("autosell", data.doAutosell());

            jsonMap.put(loc.hashCode() + "", dataMap);
        }

        File file = new File(SkycadeChunkCollectorsPlugin.getInstance().getDataFolder(), "collectors.json");
        String json = gson.toJson(jsonMap);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isAutosellEnabled() {
        return autosellEnabled;
    }
}
