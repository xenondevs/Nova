package xyz.xenondevs.nova.api.block;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.xenondevs.nova.api.material.NovaMaterial;

import java.util.List;

public interface BlockManager {
    
    /**
     * Checks if there is a {@link NovaBlockState} at that location.
     *
     * @param location The location to check.
     * @return If there is a {@link NovaBlockState} at that location.
     */
    boolean hasBlock(@NotNull Location location);
    
    /**
     * Gets the {@link NovaBlockState} at that location.
     *
     * @param location The location of the block.
     * @return The Nova block state or null if there isn't one at that location.
     */
    @Nullable NovaBlockState getBlock(@NotNull Location location);
    
    /**
     * Places the block at that location.
     *
     * @param location The location where the block should be placed.
     * @param block The type of the block
     */
    default void placeBlock(@NotNull Location location, @NotNull NovaBlock block) {
        placeBlock(location, block, null);
    }
    
    /**
     * Places the material at that location.
     *
     * @param location The location where the block should be placed.
     * @param material The material of the block.
     * @throws IllegalArgumentException If the material is not a block.
     * @deprecated Use {@link #placeBlock(Location, NovaBlock)} instead. 
     */
    @Deprecated
    default void placeBlock(@NotNull Location location, @NotNull NovaMaterial material) {
        placeBlock(location, material, null);
    }
    
    /**
     * Places the block at that location.
     *
     * @param location The location where the block should be placed.
     * @param block The type of the block.
     * @param source   The source of this block placement. Could be a player, tile-entity or similar.
     */
    default void placeBlock(@NotNull Location location, @NotNull NovaBlock block, @Nullable Object source) {
        placeBlock(location, block, source, true);
    }
    
    /**
     * Places the material at that location.
     *
     * @param location The location where the block should be placed.
     * @param material The material of the block.
     * @param source   The source of this block placement. Could be a player, tile-entity or similar.
     * @throws IllegalArgumentException If the material is not a block.
     * @deprecated Use {@link #placeBlock(Location, NovaBlock, Object)} instead.
     */
    @Deprecated
    default void placeBlock(@NotNull Location location, @NotNull NovaMaterial material, @Nullable Object source) {
        placeBlock(location, material, source, true);
    }
    
    /**
     * Places the block at that location.
     *
     * @param location The location where the block should be placed.
     * @param block The type of the block.
     * @param source    The source of this block placement. Could be a player, tile-entity or similar.
     * @param playSound If block breaking sounds should be placed.
     */
    void placeBlock(@NotNull Location location, @NotNull NovaBlock block, @Nullable Object source, boolean playSound);
    
    
    /**
     * Places the material at that location.
     *
     * @param location The location where the block should be placed.
     * @param material The material of the block.
     * @param source    The source of this block placement. Could be a player, tile-entity or similar.
     * @param playSound If block breaking sounds should be placed.
     * @throws IllegalArgumentException If the material is not a block.
     * @deprecated Use {@link #placeBlock(Location, NovaBlock, Object, boolean)} instead.
     */
    @Deprecated
    void placeBlock(@NotNull Location location, @NotNull NovaMaterial material, @Nullable Object source, boolean playSound);
    
    /**
     * Gets the drops of the Nova block at that location or null if there is no Nova block there.
     *
     * @param location The location of the Nova block.
     * @return The list of drops or null if there is no block from Nova at that location.
     */
    default @Nullable List<@NotNull ItemStack> getDrops(@NotNull Location location) {
        return getDrops(location, null);
    }
    
    /**
     * Gets the drops of the Nova block at that location as if it was mined with the given tool
     * or null if there is no Nova block there.
     *
     * @param location The location of the Nova block.
     * @param tool     The tool that should be used.
     * @return The list of drops or null if there is no block from Nova at that location.
     */
    default @Nullable List<@NotNull ItemStack> getDrops(@NotNull Location location, @Nullable ItemStack tool) {
        return getDrops(location, null, tool);
    }
    
    /**
     * Gets the drops of the Nova block at that location as if it was mined by source with the
     * given tool or null if there is no Nova block there.
     *
     * @param location The location of the Nova block.
     * @param source   The source of this action. Could be a player, tile-entity or similar.
     * @param tool     The tool that should be used.
     * @return The list of drops or null if there is no block from Nova at that location.
     */
    @Nullable List<@NotNull ItemStack> getDrops(@NotNull Location location, @Nullable Object source, @Nullable ItemStack tool);
    
    /**
     * Removes the Nova block at that location.
     *
     * @param location The location of the block to remove.
     * @return If there was a Nova block at that location and the removal was successful.
     */
    default boolean removeBlock(@NotNull Location location) {
        return removeBlock(location, null);
    }
    
    /**
     * Removes the Nova block at that location as if it was destroyed by source.
     *
     * @param location The location of the block to remove.
     * @param source   The source of the block removal.
     * @return If there was a Nova block at that location and the removal was successful.
     */
    default boolean removeBlock(@NotNull Location location, @Nullable Object source) {
        return removeBlock(location, source, true, true);
    }
    
    /**
     * Removes the Nova block at that location as if it was destroyed by source.
     *
     * @param location      The location of the block to remove.
     * @param source        The source of the block removal.
     * @param playSound     If block breaking sounds should be played.
     * @param showParticles If block breaking particles should be displayed.
     * @return If there was a Nova block at that location and the removal was successful.
     * @deprecated Break sound and particles are not independent from one another. Use {@link #removeBlock(Location, Object, boolean)} instead.
     */
    @Deprecated
    default boolean removeBlock(@NotNull Location location, @Nullable Object source, boolean playSound, boolean showParticles) {
        return removeBlock(location, source, playSound || showParticles);
    }
    
    /**
     * Removes the Nova block at that location as if it was destroyed by source.
     *
     * @param location      The location of the block to remove.
     * @param source        The source of the block removal.
     * @param breakEffects  If block breaking effects should be played.
     * @return If there was a Nova block at that location and the removal was successful.
     */
    boolean removeBlock(@NotNull Location location, @Nullable Object source, boolean breakEffects);
    
}