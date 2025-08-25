package io.github.alvinnorin.steinerportals;

import java.util.HashSet;

/**
 * Specialized HashSet for storing protected locations, efficiently keep their neighbourhood relationships consistent.
 */
public class ProtectedLocationSet extends HashSet<ProtectedLocation> {

    private final int neighbourRadius = 500;     // If a protected location is within this block radius from another protected location, they are considered a neighbourhood, and their protection radius are added to each other

    @Override
    public boolean add(ProtectedLocation location) {
        // Update neighbourhood relationships
        for (ProtectedLocation existingLocation : this)
            if (areWithinRange(location, existingLocation, neighbourRadius)
                    && !location.getOwner().equals(existingLocation.getOwner())) {  // Locations owned by the same entity are not considered neighbours, and their protection radius are not added to each other. This is to prevent single users from amplifying their exclusion radius by simply setting more homes at the same spot.
                location.getNeighbours().add(existingLocation);
                existingLocation.getNeighbours().add(location);
            }
        return super.add(location);
    }

    @Override
    public boolean remove(Object location) {
        boolean returnValue = super.remove(location);
        // Update neighbourhood relationships
        if (returnValue)
            if (location instanceof ProtectedLocation) {
                ProtectedLocation protectedLocation = (ProtectedLocation) location;
                for (ProtectedLocation existingLocation : this)
                    existingLocation.getNeighbours().remove(location);
                protectedLocation.getNeighbours().clear();
            }
        return returnValue;
    }

    public boolean isLocationProtected(ProtectedLocation location) {
        for (ProtectedLocation existingLocation : this)
            if (areWithinRange(location, existingLocation, Math.max(location.getProtectionRadius(),existingLocation.getProtectionRadius())))
                return true;
        return false;
    }

    private boolean areWithinRange(ProtectedLocation location1, ProtectedLocation location2, int radius) {
        if (!location1.getWorld().equals(location2.getWorld()))
            return false;
        int lowerX = location2.getX() - radius;
        int upperX = location2.getX() + radius;
        int lowerZ = location2.getZ() - radius;
        int upperZ = location2.getZ() + radius;
        if (location1.getX() > lowerX && location1.getX() < upperX)
            return location1.getZ() > lowerZ && location1.getZ() < upperZ;
        return false;
    }

}
