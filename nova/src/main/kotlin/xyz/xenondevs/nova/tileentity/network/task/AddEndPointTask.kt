package xyz.xenondevs.nova.tileentity.network.task

import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.collections.toEnumSet
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.tileentity.network.ProtoNetwork
import xyz.xenondevs.nova.tileentity.network.node.NetworkBridge
import xyz.xenondevs.nova.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.type.NetworkType
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntity
import xyz.xenondevs.nova.world.format.NetworkState
import xyz.xenondevs.nova.world.format.chunk.NetworkEndPointData

internal class AddEndPointTask(
    state: NetworkState,
    node: NetworkEndPoint,
    updateNodes: Boolean
) : AddNodeTask<NetworkEndPoint>(state, node, updateNodes) {
    
    override fun add() {
        state.setEndPointData(
            node.pos,
            NetworkEndPointData(node.owner)
        )
        
        val clustersToEnlarge = HashSet<ProtoNetwork>()
        
        for (networkType in NovaRegistries.NETWORK_TYPE) {
            var allowedFaces = state.getAllowedFaces(node, networkType)
            if (allowedFaces.isEmpty())
                continue
            allowedFaces = allowedFaces.toEnumSet().also { result.removeProtected(it) }
            
            for ((face, neighborNode) in state.getNearbyNodes(node.pos, allowedFaces)) {
                // do not allow networks between two vanilla tile entities
                if (node is VanillaTileEntity && neighborNode is VanillaTileEntity)
                    continue
                
                val success = when (neighborNode) {
                    is NetworkBridge -> tryConnectToBridge(neighborNode, networkType, face, clustersToEnlarge)
                    is NetworkEndPoint -> tryConnectToEndPoint(neighborNode, networkType, face, clustersToEnlarge)
                }
                
                if (success) {
                    nodesToUpdate += neighborNode
                }
            }
        }
        
        for (network in clustersToEnlarge) {
            network.enlargeCluster(node)
        }
    }
    
    private fun tryConnectToBridge(
        bridge: NetworkBridge,
        networkType: NetworkType, face: BlockFace,
        clustersToEnlarge: MutableSet<ProtoNetwork>
    ): Boolean {
        if (face.oppositeFace in state.getAllowedFaces(bridge, networkType)) {
            state.connectEndPointToBridge(node, bridge, networkType, face, clustersToEnlarge)
            return true
        }
        
        return false
    }
    
    private fun tryConnectToEndPoint(
        endPoint: NetworkEndPoint,
        networkType: NetworkType, face: BlockFace,
        clustersToEnlarge: MutableSet<ProtoNetwork>
    ): Boolean {
        if (face.oppositeFace in state.getAllowedFaces(endPoint, networkType)) {
            state.connectEndPointToEndPoint(node, endPoint, networkType, face, clustersToEnlarge)
            return true
        }
        
        return false
    }
    
}