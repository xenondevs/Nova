package xyz.xenondevs.nova.tileentity.network.node

import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.collections.enumSet
import xyz.xenondevs.nova.tileentity.network.type.NetworkConnectionType

/**
 * An [EndPointDataHolder] that has [EndPointContainers][EndPointContainer] assigned to its block faces.
 * Also has a channel configuration and insert / extract priorities.
 */
interface ContainerEndPointDataHolder<C : EndPointContainer> : EndPointDataHolder {
    
    /**
     * The [BlockFaces][BlockFace] that can never have a connection.
     */
    val blockedFaces: Set<BlockFace>
    
    /**
     * Stores all available [C] and their allowed [NetworkConnectionTypes][NetworkConnectionType].
     */
    val containers: Map<C, NetworkConnectionType>
    
    /**
     * Stores the currently configured [NetworkConnectionType] per [BlockFace].
     */
    val connectionConfig: MutableMap<BlockFace, NetworkConnectionType>
    
    /**
     * Stores which [C] is accessible from what [BlockFace].
     */
    val containerConfig: MutableMap<BlockFace, C>
    
    /**
     * Stores the selected channels per [BlockFace].
     */
    val channels: MutableMap<BlockFace, Int>
    
    /**
     * Stores the insertion priorities per [BlockFace].
     */
    val insertPriorities: MutableMap<BlockFace, Int>
    
    /**
     * Stores the extraction priorities per [BlockFace].
     */
    val extractPriorities: MutableMap<BlockFace, Int>
    
    override val allowedFaces: Set<BlockFace>
        get() = connectionConfig.mapNotNullTo(enumSet()) { (face, type) ->
            if (type != NetworkConnectionType.NONE) face else null
        }
    
}