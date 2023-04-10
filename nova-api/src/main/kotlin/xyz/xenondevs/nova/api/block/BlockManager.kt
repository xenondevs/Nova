package xyz.xenondevs.nova.api.block

import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.material.NovaMaterial

@Suppress("DEPRECATION")
interface BlockManager {
    
    /**
     * Checks if there is a [NovaBlockState] at that [location].
     * 
     * @param location The location to check.
     * @return If there is a [NovaBlockState] at that location.
     */
    fun hasBlock(location: Location): Boolean
    
    /**
     * Gets the [NovaBlockState] at that [location].
     *
     * @param location The location of the block.
     * @return The Nova block state or null if there isn't one at that location.
     */
    fun getBlock(location: Location): NovaBlockState?
    
    /**
     * Places the [block] at that [location].
     *
     * @param location The location where the block should be placed.
     * @param block The material of the block.
     */
    fun placeBlock(location: Location, block: NovaBlock) = placeBlock(location, block, null)
    
    /**
     * Places the [material] at that [location].
     *
     * @param location The location where the block should be placed.
     * @param material The material of the block.
     * @throws IllegalArgumentException If the [material] is not a block.
     */
    @Deprecated("Use NovaBlock instead", ReplaceWith("placeBlock(location, block)"))
    fun placeBlock(location: Location, material: NovaMaterial) = placeBlock(location, material, null)
    
    /**
     * Places the [block] at that [location].
     *
     * @param location The location where the block should be placed.
     * @param block The material of the block.
     * @param source The source of this block placement. Could be a player, tile-entity or similar.
     */
    fun placeBlock(location: Location, block: NovaBlock, source: Any?) = placeBlock(location, block, source, true)
    
    /**
     * Places the [material] at that [location].
     *
     * @param location The location where the block should be placed.
     * @param material The material of the block.
     * @param source The source of this block placement. Could be a player, tile-entity or similar.
     * @throws IllegalArgumentException If the [material] is not a block.
     */
    @Deprecated("Use NovaBlock instead", ReplaceWith("placeBlock(location, block, source)"))
    fun placeBlock(location: Location, material: NovaMaterial, source: Any?) = placeBlock(location, material, source, true)
    
    /**
     * Places the [block] at that [location].
     *
     * @param source The source of this block placement. Could be a player, tile-entity or similar.
     * @param playSound If block breaking sounds should be placed.
     */
    fun placeBlock(location: Location, block: NovaBlock, source: Any?, playSound: Boolean)
    
    /**
     * Places the [material] at that [location].
     *
     * @param source The source of this block placement. Could be a player, tile-entity or similar.
     * @param playSound If block breaking sounds should be placed.
     * @throws IllegalArgumentException If the [material] is not a block.
     */
    @Deprecated("Use NovaBlock instead", ReplaceWith("placeBlock(location, block, source, playSound)"))
    fun placeBlock(location: Location, material: NovaMaterial, source: Any?, playSound: Boolean)
    
    /**
     * Gets the drops of the Nova block at that [location] or null if there is no Nova block there.
     *
     * @param location The location of the Nova block.
     * @return The list of drops or null if there is no block from Nova at that location.
     */
    fun getDrops(location: Location): List<ItemStack>? = getDrops(location, null)
    
    /**
     * Gets the drops of the Nova block at that [location] as if it was mined with the given [tool]
     * or null if there is no Nova block there.
     *
     * @param location The location of the Nova block.
     * @param tool The tool that should be used.
     * @return The list of drops or null if there is no block from Nova at that location.
     */
    fun getDrops(location: Location, tool: ItemStack?): List<ItemStack>? = getDrops(location, null, null)
    
    /**
     * Gets the drops of the Nova block at that [location] as if it was mined by [source] with the
     * given [tool] or null if there is no Nova block there.
     *
     * @param location The location of the Nova block.
     * @param source The source of this action. Could be a player, tile-entity or similar.
     * @param tool The tool that should be used.
     * @return The list of drops or null if there is no block from Nova at that location.
     */
    fun getDrops(location: Location, source: Any?, tool: ItemStack?): List<ItemStack>?
    
    /**
     * Removes the Nova block at that [location].
     *
     * @param location The location of the block to remove.
     * @return If there was a Nova block at that location and the removal was successful.
     */
    fun removeBlock(location: Location): Boolean = removeBlock(location, null)
    
    /**
     * Removes the Nova block at that [location] as if it was destroyed by [source].
     *
     * @param location The location of the block to remove.
     * @param source The source of the block removal.
     * @return If there was a Nova block at that location and the removal was successful.
     */
    fun removeBlock(location: Location, source: Any?) = removeBlock(location, source, true)
    
    /**
     * Removes the Nova block at that [location] as if it was destroyed by [source].
     *
     * @param location The location of the block to remove.
     * @param source The source of the block removal.
     * @param playSound If block breaking sounds should be played.
     * @param showParticles If block breaking particles should be displayed.
     * @return If there was a Nova block at that location and the removal was successful.
     */
    @Deprecated("Break sound and particles are not independent from one another", ReplaceWith("removeBlock(location, source, playSound || showParticles)"))
    fun removeBlock(location: Location, source: Any?, playSound: Boolean, showParticles: Boolean): Boolean =
        removeBlock(location, source, playSound || showParticles)
    
    /**
     * Removes the Nova block at that [location] as if it was destroyed by [source].
     *
     * @param location The location of the block to remove.
     * @param source The source of the block removal.
     * @param breakEffects If break effects such as sounds and particles should be played.
     * @return If there was a Nova block at that location and the removal was successful.
     */
    fun removeBlock(location: Location, source: Any?, breakEffects: Boolean): Boolean
    
}