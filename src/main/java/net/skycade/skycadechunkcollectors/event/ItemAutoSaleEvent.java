package net.skycade.skycadechunkcollectors.event;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ItemAutoSaleEvent extends Event implements Cancellable {

    private static HandlerList handlerList = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    private final Block block;
    private final OfflinePlayer player;
    private final double initialCost;
    private final Material material;
    private final short durability;
    private final int amount;
    private double newCost;

    private boolean isCancelled = false;

    public ItemAutoSaleEvent(Block block, Material material, short durability, int amount, OfflinePlayer player, double initialCost) {
        this.block = block;
        this.player = player;
        this.initialCost = initialCost;
        this.material = material;
        this.durability = durability;
        this.amount = amount;

        this.newCost = initialCost;
    }

    public Block getBlock() {
        return block;
    }

    public OfflinePlayer getPlayer() {
        return player;
    }

    public double getInitialCost() {
        return initialCost;
    }

    public double getNewCost() {
        return newCost;
    }

    public void setNewCost(double newCost) {
        this.newCost = newCost;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
    }

    public Material getMaterial() {
        return material;
    }

    public short getDurability() {
        return durability;
    }

    public int getAmount() {
        return amount;
    }
}
