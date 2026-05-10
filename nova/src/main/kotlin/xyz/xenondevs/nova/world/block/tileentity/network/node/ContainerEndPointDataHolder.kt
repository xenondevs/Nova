package xyz.xenondevs.nova.world.block.tileentity.network.node

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.util.CubeFaceMap
import xyz.xenondevs.nova.util.CubeFaceSet
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType

/**
 * An [EndPointDataHolder] that has [EndPointContainers][EndPointContainer] assigned to its block faces.
 * Also has a channel configuration and insert / extract priorities.
 */
interface ContainerEndPointDataHolder<C : EndPointContainer> : EndPointDataHolder {
    
    /**
     * The [BlockFaces][BlockFace] that can never have a connection.
     */
    val blockedFaces: CubeFaceSet
    
    /**
     * Stores all available [C] and their allowed [NetworkConnectionTypes][NetworkConnectionType].
     */
    val containers: Map<C, NetworkConnectionType>
    
    /**
     * Stores the currently configured [NetworkConnectionType] per [BlockFace].
     */
    var connectionConfig: CubeFaceMap<NetworkConnectionType>
    
    /**
     * Stores which [C] is accessible from what [BlockFace].
     */
    var containerConfig: CubeFaceMap<C?>
    
    /**
     * Stores the selected channels per [BlockFace].
     */
    var channels: CubeFaceMap<Int>
    
    /**
     * Stores the insertion priorities per [BlockFace].
     */
    var insertPriorities: CubeFaceMap<Int>
    
    /**
     * Stores the extraction priorities per [BlockFace].
     */
    var extractPriorities: CubeFaceMap<Int>
    
    override val allowedFaces: CubeFaceSet
        get() = connectionConfig.mapToCubeFaceSet { it != NetworkConnectionType.NONE }
    
}