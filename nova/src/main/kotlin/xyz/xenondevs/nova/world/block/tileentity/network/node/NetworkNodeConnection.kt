package xyz.xenondevs.nova.world.block.tileentity.network.node

import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.collections.enumSet
import xyz.xenondevs.nova.world.block.tileentity.network.Network

/**
 * Represents a connection of a [NetworkNode] to a [Network].
 */
sealed interface NetworkNodeConnection {
    
    /**
     * The [NetworkNode] that is connected.
     */
    val node: NetworkNode
    
    /**
     * The [BlockFaces][BlockFace] with which the [node] is connected.
     */
    val faces: Set<BlockFace>
    
    operator fun component1(): NetworkNode = node
    operator fun component2(): Set<BlockFace> = faces
    
}

/**
 * An implementation of [NetworkNodeConnection] with a mutable [faces] set.
 *
 * @param node The [NetworkNode] that is connected.
 * @param faces The [BlockFaces][BlockFace] with which the [node] is connected.
 */
data class MutableNetworkNodeConnection(
    override val node: NetworkNode,
    override val faces: MutableSet<BlockFace> = enumSet()
) : NetworkNodeConnection