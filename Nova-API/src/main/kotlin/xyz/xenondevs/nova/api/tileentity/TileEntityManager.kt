package xyz.xenondevs.nova.api.tileentity

import org.bukkit.Location

interface TileEntityManager {
    
    /**
     * Gets the [TileEntity] at that [location] or null if there isn't one
     */
    @Deprecated("Inconsistent name", replaceWith = ReplaceWith("getTileEntity"))
    fun getTileEntityAt(location: Location): TileEntity? = getTileEntity(location)
    
    /**
     * Gets the [TileEntity] at that [location] or null if there isn't one
     */
    fun getTileEntity(location: Location): TileEntity?
    
}