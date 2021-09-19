package xyz.xenondevs.nova.tileentity.network

import org.bukkit.block.BlockFace

/**
 * Holds the [NetworkType]-specific data for [End Points][NetworkEndPoint].
 */
interface EndPointDataHolder {
    
    /**
     * The [NetworkEndPoint] of this [EndPointDataHolder]
     */
    val endPoint: NetworkEndPoint
    
    /**
     * A set of [Block Faces][BlockFace] where connections are allowed.
     */
    val allowedFaces: Set<BlockFace>
    
    /**
     * Saves the data.
     */
    fun saveData()
    
}