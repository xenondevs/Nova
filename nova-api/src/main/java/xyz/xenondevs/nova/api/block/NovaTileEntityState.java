package xyz.xenondevs.nova.api.block;

import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.nova.api.tileentity.TileEntity;

public interface NovaTileEntityState extends NovaBlockState {
    
    /**
     * Gets the {@link TileEntity} represented by this {@link NovaTileEntityState}.
     *
     * @return The {@link TileEntity} represented by this {@link NovaTileEntityState}.
     */
    @NotNull TileEntity getTileEntity();
    
}