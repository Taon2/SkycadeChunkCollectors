package net.skycade.skycadechunkcollectors.data;

import java.util.Objects;

public class ChunkLocation implements Comparable<ChunkLocation> {
    private final int x;
    private final int z;
    private final String world;

    public ChunkLocation(int x, int z, String world) {
        this.x = x;
        this.z = z;
        this.world = world;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public String getWorld() {
        return world;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChunkLocation that = (ChunkLocation) o;
        return x == that.x &&
                z == that.z &&
                Objects.equals(world, that.world);
    }

    @Override
    public int hashCode() {
        return (this.x << 16) | (0x0000FFFF & this.z);
    }

    @Override
    public int compareTo(ChunkLocation o) {
        if (o == null) throw new NullPointerException("object is null");

        return Integer.compare(this.hashCode(), o.hashCode());
    }
}
