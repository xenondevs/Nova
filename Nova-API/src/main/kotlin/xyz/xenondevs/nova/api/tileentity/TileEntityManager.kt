package xyz.xenondevs.nova.api.tileentity

import org.bukkit.Location

interface TileEntityManager {
    
    /**
     * Gets the [TileEntity] at that [location] or null if there isn't one
     */
    fun getTileEntityAt(location: Location): TileEntity?
    
}