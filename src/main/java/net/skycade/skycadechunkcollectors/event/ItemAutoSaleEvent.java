package net.skycade.skycadechunkcollectors.event;

import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class ItemAutoSaleEvent extends Event implements Cancellable {

    private static HandlerList handlerList = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    private final Block block;
    private final OfflinePlayer player;
    private final double initialCost;
    private final ItemStack itemStack;
    private double newCost;

    private boolean isCancelled = false;

    public ItemAutoSaleEvent(Block block, ItemStack itemStack, OfflinePlayer player, double initialCost) {
        this.block = block;
        this.player = player;
        this.initialCost = initialCost;
        this.itemStack = itemStack;

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

    public ItemStack getItemStack() {
        return itemStack;
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
}
