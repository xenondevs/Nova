package xyz.xenondevs.nova.api.event.tileentity;

import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.nova.api.tileentity.TileEntity;

/**
 * A {@link TileEntity} related event.
 */
public abstract class TileEntityEvent extends Event {
    
    private final @NotNull TileEntity tileEntity;
    
    public TileEntityEvent(@NotNull TileEntity tileEntity) {
        this.tileEntity = tileEntity;
    }
    
    public @NotNull TileEntity getTileEntity() {
        return tileEntity;
    }
    
    @Override
    public @NotNull String toString() {
        return "TileEntityEvent{" +
            "tileEntity=" + tileEntity +
            '}';
    }
    
}