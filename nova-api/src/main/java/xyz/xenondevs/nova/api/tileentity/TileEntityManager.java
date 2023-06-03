package xyz.xenondevs.nova.api.tileentity;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface TileEntityManager {
    
    /**
     * Gets the {@link TileEntity} at that {@link Location} or null if there isn't one.
     *
     * @return The {@link TileEntity} at that {@link Location} or null if there isn't one.
     * @deprecated Inconsistent naming. Use {@link #getTileEntity(Location)} instead.
     */
    @Deprecated
    default @Nullable TileEntity getTileEntityAt(@NotNull Location location) {
        return getTileEntity(location);
    }
    
    /**
     * Gets the {@link TileEntity} at that {@link Location} or null if there isn't one.
     *
     * @return The {@link TileEntity} at that {@link Location} or null if there isn't one.
     */
    @Nullable TileEntity getTileEntity(@NotNull Location location);
    
}