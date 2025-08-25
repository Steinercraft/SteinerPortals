package io.github.alvinnorin.steinerportals;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

import java.util.*;

public class ProtectedLocation {

    private final int exclusionRadius = 5000;    // The exclusion radius every protected location starts off with

    @Getter
    private UUID world;

    @Getter
    private int x;

    @Getter
    private int z;

    @Getter @Setter
    private UUID owner;

    @Getter
    private final HashSet<ProtectedLocation> neighbours = new HashSet<>();

    public ProtectedLocation(UUID world, int x, int y, UUID owner) throws NullPointerException {
        if (world == null)
            throw new NullPointerException("World cannot be null");
        this.world = world;
        this.x = x;
        this.z = y;
        this.owner = owner;
    }

    public ProtectedLocation(Location location) throws NullPointerException {
        if (location == null)
            throw new NullPointerException("World cannot be null");
        this.world = Objects.requireNonNull(location.getWorld()).getUID();
        this.x = location.getBlockX();
        this.z = location.getBlockZ();
    }

    public int getProtectionRadius() {
        return exclusionRadius + (neighbours.size() * exclusionRadius);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z, world);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ProtectedLocation that = (ProtectedLocation) obj;
        return x == that.x && z == that.z && world.equals(that.world);
    }

}
