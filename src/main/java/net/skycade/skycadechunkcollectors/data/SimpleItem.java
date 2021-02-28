package net.skycade.skycadechunkcollectors.data;

import org.bukkit.Material;

public class SimpleItem {
    private Material material;
    private short data;
    private int amount;

    public SimpleItem(Material material, short data, int amount) {
        this.material = material;
        this.data = data;
        this.amount = amount;
    }

    public Material getMaterial() {
        return material;
    }

    public short getData() {
        return data;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
