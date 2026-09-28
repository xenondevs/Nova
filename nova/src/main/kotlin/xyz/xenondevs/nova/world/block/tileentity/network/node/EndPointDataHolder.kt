package xyz.xenondevs.nova.world.block.tileentity.network.node

import xyz.xenondevs.nova.world.*

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.util.CubeFaceSet

/**
 * Holds data for [NetworkEndPoints][NetworkEndPoint].
 */
interface EndPointDataHolder {
    
    /**
     * A set of [BlockFaces][BlockFace] where connections are allowed.
     */
    val allowedFaces: CubeFaceSet
    
}
