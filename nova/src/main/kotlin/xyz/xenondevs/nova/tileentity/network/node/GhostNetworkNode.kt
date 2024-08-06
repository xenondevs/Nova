package xyz.xenondevs.nova.tileentity.network.node

import net.minecraft.resources.ResourceLocation
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import xyz.xenondevs.nova.world.BlockPos
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
                is NetworkBridge -> GhostNetworkBridge(node.pos, node.owner, node.typeId)
                is NetworkEndPoint -> GhostNetworkEndPoint(node.pos, node.owner)
            }
        
        /**
         * Creates a [NetworkNode] from the given [pos] and [data].
         */
        fun fromData(pos: BlockPos, data: NetworkNodeData): NetworkNode =
            when (data) {
                is NetworkBridgeData -> GhostNetworkBridge(pos, data)
                is NetworkEndPointData -> GhostNetworkEndPoint(pos, data)
            }
        
    }
    
}

/**
 * A ghost [NetworkBridge].
 *
 * Takes the place of all regular [NetworkBridges][NetworkBridge] after they've been unloaded.
 */
internal class GhostNetworkBridge(
    override val pos: BlockPos,
    override val owner: OfflinePlayer?,
    override val typeId: ResourceLocation
) : NetworkBridge, GhostNetworkNode {
    
    override val isValid = true
    override val linkedNodes: Set<NetworkNode> = emptySet()
    
    constructor(pos: BlockPos, data: NetworkBridgeData) : this(pos, Bukkit.getOfflinePlayer(data.owner), data.typeId)
    
    override fun hashCode(): Int {
        return pos.hashCode()
    }
    
    override fun equals(other: Any?): Boolean {
        return other is GhostNetworkBridge && other.pos == pos
    }
    
}

/**
 * A ghost [NetworkEndPoint].
 *
 * Takes the place of all regular [NetworkEndPoints][NetworkEndPoint] after they've been unloaded.
 */
internal class GhostNetworkEndPoint(
    override val pos: BlockPos,
    override val owner: OfflinePlayer?,
) : NetworkEndPoint, GhostNetworkNode {
    
    override val isValid = true
    override val holders: Set<EndPointDataHolder> = emptySet()
    override val linkedNodes: Set<NetworkNode> = emptySet()
    
    constructor(pos: BlockPos, data: NetworkEndPointData) : this(pos, Bukkit.getOfflinePlayer(data.owner))
    
    override fun hashCode(): Int {
        return pos.hashCode()
    }
    
    override fun equals(other: Any?): Boolean {
        return other is GhostNetworkEndPoint && other.pos == pos
    }
    
}
