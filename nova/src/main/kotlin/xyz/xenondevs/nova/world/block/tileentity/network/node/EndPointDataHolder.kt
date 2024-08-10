package xyz.xenondevs.nova.world.block.tileentity.network.node

import org.bukkit.block.BlockFace

/**
 * Holds data for [NetworkEndPoints][NetworkEndPoint].
 */
interface EndPointDataHolder {
    
    /**
     * A set of [BlockFaces][BlockFace] where connections are allowed.
     */
    val allowedFaces: Set<BlockFace>
    
}