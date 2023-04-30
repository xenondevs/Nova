package xyz.xenondevs.nova.api.block;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.xenondevs.nova.api.data.NamespacedId;

public interface NovaBlockRegistry {
    
    /**
     * Gets the {@link NovaBlock} with the specified id.
     * @param id The id of the block.
     * @return The block with the specified id.
     * @throws IllegalArgumentException If there is no block with the specified id.
     */
    @NotNull NovaBlock get(@NotNull String id);
    
    /**
     * Gets the {@link NovaBlock} with the specified id.
     * @param id The id of the block.
     * @return The block with the specified id.
     * @throws IllegalArgumentException If there is no block with the specified id.
     */
    @NotNull NovaBlock get(@NotNull NamespacedId id);
    
    /**
     * Gets the {@link NovaBlock} with the specified id, or null if there is none.
     * @param id The id of the block.
     * @return The block with the specified id, or null if there is none.
     */
    @Nullable NovaBlock getOrNull(@NotNull String id);
    
    /**
     * Gets the {@link NovaBlock} with the specified id, or null if there is none.
     * @param id The id of the block.
     * @return The block with the specified id, or null if there is none.
     */
    @Nullable NovaBlock getOrNull(@NotNull NamespacedId id);
    
}
