package xyz.xenondevs.nova.api.tileentity;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
    @Nullable
    TileEntity getTileEntity(@NotNull Location location);
    
    /**
     * Gets all loaded {@link TileEntity TileEntities} in the specified {@link Chunk}.
     *
     * @param chunk The {@link Chunk} to get the {@link TileEntity TileEntities} from.
     * @return All loaded {@link TileEntity TileEntities} in the specified {@link Chunk}.
     */
    @NotNull
    List<@NotNull TileEntity> getTileEntities(@NotNull Chunk chunk);
    
    /**
     * Gets all loaded {@link TileEntity TileEntities} in the specified {@link World}.
     *
     * @param world The {@link World} to get the {@link TileEntity TileEntities} from.
     * @return All loaded {@link TileEntity TileEntities} in the specified {@link World}.
     */
    @NotNull
    List<@NotNull TileEntity> getTileEntities(@NotNull World world);
    
    /**
     * Gets all loaded {@link TileEntity TileEntities}.
     *
     * @return All loaded {@link TileEntity TileEntities}.
     */
    @NotNull
    List<@NotNull TileEntity> getTileEntities();
    
}