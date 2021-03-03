package net.skycade.skycadechunkcollectors.data;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class BlockData implements Comparable<BlockData> {
    private UUID uuid;
    private Location location;
    private List<Material> types;
    private List<SimpleItem> storage;
    private int storagePages;
    private boolean doAutosell;
    private List<UUID> storageViewers;
    private LinkedHashSet<Location> linkedChests = new LinkedHashSet<>();

    public BlockData(UUID uuid, Location location, int tier) {
        this.uuid = uuid;
        this.location = location;
        this.types = new ArrayList<>();
        this.storage = new ArrayList<>();
        this.storagePages = tier;
        this.doAutosell = ChunkCollectorManager.isAutosellEnabled();
        this.storageViewers = new ArrayList<>();
    }

    public BlockData(UUID uuid, Location location, List<Material> types, List<SimpleItem> storage, int storagePages, boolean doAutosell) {
        this.uuid = uuid;
        this.location = location;
        this.types = types;
        this.storage = storage;
        this.storagePages = storagePages;
        this.doAutosell = doAutosell;
        this.storageViewers = new ArrayList<>();
    }

    @Override
    public int compareTo(BlockData o) {
        if (o == null) throw new NullPointerException("object is null");

        int a = Integer.compare(((Double) this.getLocation().getY()).intValue(), ((Double) o.getLocation().getY()).intValue());

        if (a == 0) return Integer.compare(this.hashCode(), o.hashCode());
        return a;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Location getLocation() {
        return location;
    }

    public void addType(Material type) {
        types.add(type);
    }

    public void removeType(Material type) {
        types.remove(type);
    }

    public boolean containsType(Material material) {
        return types.contains(material);
    }

    public List<Material> getTypes() {
        return types;
    }

    /**
     * Adds an itemstack into storage, accounting for the capacity of the storage
     * @param item The item to be added to storage
     */
    public void addToStorage(ItemStack item) {
        int amount = item.getAmount();

        // add the full item if space
        if (getStoredStacks() < getMaxSlots()) {
            for (SimpleItem stored : storage) {
                if (stored.getMaterial() == item.getType()
                        && stored.getData() == item.getDurability()) {
                    stored.setAmount(stored.getAmount() + amount);
                    return;
                }
            }

            // add a new item, because it wasn't in storage yet
            SimpleItem simpleItem = new SimpleItem(item.getType(), item.getDurability(), amount);
            storage.add(simpleItem);
        } else {
            // try "topping off" a stack
            for (SimpleItem stored : storage) {
                if (stored.getAmount() <= 0) continue;

                if (stored.getMaterial() == item.getType()
                        && stored.getData() == item.getDurability()) {
                    int maxStackSize = stored.getMaterial().getMaxStackSize();
                    int storedAmount = stored.getAmount();
                    int space = maxStackSize - (storedAmount % maxStackSize);

                    if (storedAmount % maxStackSize == 0) return; // this item is already "topped off"

                    if (space > amount) {
                        // top off the stack with the full item amount, because it can definitely fit
                        stored.setAmount(storedAmount + amount);
                    } else {
                        // top off the stack with the remainder, because the item amount wont fit
                        stored.setAmount(storedAmount + space);
                    }

                    // if it gets here, the item was handled
                    return;
                }
            }
        }
    }

    /**
     * Remove an itemstack from storage
     * @param item The itemstack to be removed
     */
    public void removeFromStorage(ItemStack item) {
        // remove from existing stack
        List<SimpleItem> toRemove = new ArrayList<>();
        for (SimpleItem stored : storage) {
            if (stored.getMaterial() == item.getType()
                    && stored.getData() == item.getDurability()) {
                if (stored.getAmount() - item.getAmount() <= 0) {
                    // remove the stack from storage if it has no items left
                    toRemove.add(stored);
                } else {
                    // reduce an existing stack
                    stored.setAmount(stored.getAmount() - item.getAmount());
                }

                break; // exit the loop
            }
        }

        storage.removeAll(toRemove);
    }

    /**
     * Keeps a set list of items in storage, removing all others
     * @param items The items to be kept in storage
     */
    public void keepInStorage(List<ItemStack> items) {
        List<SimpleItem> toKeep = new ArrayList<>();
        for (ItemStack item : items) {
            for (SimpleItem stored : toKeep) {
                // if the item exists in storage already, add the amounts
                if (stored.getMaterial() == item.getType()
                        && stored.getData() == item.getDurability()) {
                    stored.setAmount(stored.getAmount() + item.getAmount());
                    break; // exit the loop
                }
            }

            // add a new item because it is missing
            toKeep.add(new SimpleItem(item.getType(), item.getDurability(), item.getAmount()));
        }

        this.storage = toKeep;
    }

    public List<SimpleItem> getStorage() {
        return storage;
    }

    public void addStoragePage() {
        this.storagePages++;
    }

    public int getStoragePages() {
        return storagePages;
    }

    public int getMaxSlots() {
        return 45 * storagePages;
    }

    /**
     * Calculates how many stacks of items are in storage,
     * accounting for items with a different max stack size
     * @return The amount of stored stacks in storage
     */
    public int getStoredStacks() {
        int stacks = 0;

        for (SimpleItem simpleItem : storage) {
            int amount = simpleItem.getAmount();
            int max = simpleItem.getMaterial().getMaxStackSize();

            // this works as intended even with non-full stacks, as they still take a slot
            while (amount > 0) {
                stacks++;
                amount -= max;
            }
        }

        return stacks;
    }

    /**
     * Converts the stored items into a list of itemstacks
     * @return List of itemstacks contained in storage
     */
    public List<ItemStack> storageToItemStack() {
        List<ItemStack> items = new ArrayList<>();

        for (SimpleItem simpleItem : storage) {
            int amount = simpleItem.getAmount();
            int max = simpleItem.getMaterial().getMaxStackSize();

            while (amount > 0) {
                if (amount - max >= 0) { // add a max stack
                    items.add(new ItemStack(simpleItem.getMaterial(), max, simpleItem.getData()));
                    amount -= max;
                } else { // add the remainder stack
                    items.add(new ItemStack(simpleItem.getMaterial(), amount, simpleItem.getData()));
                    amount = 0;
                }
            }
        }

        return items;
    }

    public void addLinkedChest(Location location) {
        if (location == null) return;
        Location clone = location.clone();
        if (linkedChests.contains(clone)) return;
        linkedChests.add(clone);
    }

    public void removeLinkedChest(Location location) {
        linkedChests.remove(location);
    }

    public LinkedHashSet<Location> getLinkedChests() {
        return linkedChests;
    }

    public void toggleAutosell() {
        doAutosell = !doAutosell;
    }

    public boolean doAutosell() {
        return doAutosell;
    }

    public List<UUID> getStorageViewers() {
        return storageViewers;
    }

    public void addStorageViewer(UUID uuid) {
        storageViewers.add(uuid);
    }

    public void removeStorageViewer(UUID uuid) {
        storageViewers.remove(uuid);
    }
}
