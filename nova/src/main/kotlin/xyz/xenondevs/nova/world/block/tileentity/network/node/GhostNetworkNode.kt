package xyz.xenondevs.nova.world.block.tileentity.network.node

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.block.Block
import xyz.xenondevs.nova.world.format.chunk.NetworkBridgeData
import xyz.xenondevs.nova.world.format.chunk.NetworkEndPointData
import xyz.xenondevs.nova.world.format.chunk.NetworkNodeData

/**
 * Identifier interface for all [GhostNetworkNodes][GhostNetworkNode].
 *
 * Ghost nodes take the place of regular network nodes after they've been unloaded.
 */
internal sealed interface GhostNetworkNode {
    
    companion object {
        
        /**
         * Creates a [GhostNetworkNode] from the given [node].
         */
        fun fromNode(node: NetworkNode): NetworkNode =
            when (node) {
                is NetworkBridge -> GhostNetworkBridge(node.block, node.owner, node.typeId)
                is NetworkEndPoint -> GhostNetworkEndPoint(node.block, node.owner)
            }
        
        /**
         * Creates a [NetworkNode] from the given [block] and [data].
         */
        fun fromData(block: Block, data: NetworkNodeData): NetworkNode =
            when (data) {
                is NetworkBridgeData -> GhostNetworkBridge(block, data)
                is NetworkEndPointData -> GhostNetworkEndPoint(block, data)
            }
        
    }
    
}

/**
 * A ghost [NetworkBridge].
 *
 * Takes the place of all regular [NetworkBridges][NetworkBridge] after they've been unloaded.
 */
internal class GhostNetworkBridge(
    override val block: Block,
    override val owner: OfflinePlayer?,
    override val typeId: Key
) : NetworkBridge, GhostNetworkNode {
    
    override val isValid = true
    override val linkedNodes: Set<NetworkNode> = emptySet()
    
    constructor(pos: Block, data: NetworkBridgeData) : this(pos, Bukkit.getOfflinePlayer(data.owner), data.typeId)
    
    override fun hashCode(): Int {
        return block.hashCode()
    }
    
    override fun equals(other: Any?): Boolean {
        return other is GhostNetworkBridge && other.block == block
    }
    
}

/**
 * A ghost [NetworkEndPoint].
 *
 * Takes the place of all regular [NetworkEndPoints][NetworkEndPoint] after they've been unloaded.
 */
internal class GhostNetworkEndPoint(
    override val block: Block,
    override val owner: OfflinePlayer?,
) : NetworkEndPoint, GhostNetworkNode {
    
    override val isValid = true
    override val holders: Collection<EndPointDataHolder> = emptyList()
    override val linkedNodes: Set<NetworkNode> = emptySet()
    
    constructor(pos: Block, data: NetworkEndPointData) : this(pos, Bukkit.getOfflinePlayer(data.owner))
    
    override fun hashCode(): Int {
        return block.hashCode()
    }
    
    override fun equals(other: Any?): Boolean {
        return other is GhostNetworkEndPoint && other.block == block
    }
    
}
