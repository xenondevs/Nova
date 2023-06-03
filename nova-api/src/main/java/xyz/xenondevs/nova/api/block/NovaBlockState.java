package xyz.xenondevs.nova.api.block;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.nova.api.material.NovaMaterial;

public interface NovaBlockState {
    
    /**
     * Gets the {@link NovaMaterial} of this {@link NovaBlockState}.
     *
     * @return The material of this block state.
     * @deprecated Use {@link #getBlock()} instead.
     */
    @Deprecated
    @NotNull NovaMaterial getMaterial();
    
    /**
     * Gets the {@link NovaBlock} of this {@link NovaBlockState}.
     *
     * @return The block of this block state.
     */
    @NotNull NovaBlock getBlock();
    
    /**
     * Gets the {@link Location} of this {@link NovaBlockState}.
     *
     * @return The location of this block state.
     */
    @NotNull Location getLocation();
    
}