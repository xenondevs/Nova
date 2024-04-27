package xyz.xenondevs.nova.tileentity.network.node

import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound

/**
 * Holds data for [NetworkEndPoints][NetworkEndPoint].
 */
interface EndPointDataHolder {
    
    /**
     * The [Compound] holding all data.
     */
    val compound: Compound
    
    /**
     * A set of [BlockFaces][BlockFace] where connections are allowed.
     */
    val allowedFaces: Set<BlockFace>
    
    /**
     * Saves the data to [compound].
     */
    fun saveData()
    
}