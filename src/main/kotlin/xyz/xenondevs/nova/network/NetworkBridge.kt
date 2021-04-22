package xyz.xenondevs.nova.network

import org.bukkit.block.BlockFace
import java.util.*
import kotlin.collections.Map.Entry

/**
 * Basically cables.
 */
interface NetworkBridge : NetworkNode {
    
    /**
     * The [Network]s this [NetworkBridge] is connected to.
     */
    val networks: MutableMap<NetworkType, Network>
    
    /**
     * Block faces that are allowed to transmit in a [Network].
     */
    val bridgeFaces: Set<BlockFace>
    
    /**
     * Caches the directly connected nodes.
     * Should be updated when handleNetworkUpdate is called.
     */
    val connectedNodes: Map<NetworkType, Map<BlockFace, NetworkNode>>
    
    /**
     * Called when another [NetworkNode] has ben placed
     * or broken right next to this node.
     *
     * Not called when [NetworkNode]s nearby get unloaded.
     *
     * Useful for updating the model of a cable to connect
     * to additional sides.
     */
    fun handleNetworkUpdate()
    
    /**
     * Finds the nodes directly connected to this [NetworkBridge] on each side
     * of the block.
     *
     * Will always contain the [NetworkType] keys, but only contain the [BlockFace]s that
     * actually have a [NetworkNode] connected to them.
     */
    fun findConnectedNodes(): Map<NetworkType, Map<BlockFace, NetworkNode>> {
        val connectedNodes = EnumMap<NetworkType, EnumMap<BlockFace, NetworkNode>>(NetworkType::class.java)
        NetworkType.values().forEach { connectedNodes[it] = EnumMap(BlockFace::class.java) }
        
        getNearbyNodes().forEach { (face, node) ->
            node.getNetworks(face.oppositeFace).forEach { (networkType, network) ->
                if (network == networks[networkType]) {
                    connectedNodes[networkType]!![face] = node
                }
            }
        }
        
        return connectedNodes
    }
    
    /**
     * Gets a map of attached [NetworkNode]s for each side of this [NetworkBridge].
     * The content of the maps might be the same in cases where multiple
     * [NetworkBridge]s are used to connect the same [NetworkNode]s.
     */
    fun getNetworkedNodes(networkType: NetworkType): Map<BlockFace, Set<Entry<BlockFace, NetworkNode>>> {
        val networkedNodes = EnumMap<BlockFace, Set<Entry<BlockFace, NetworkNode>>>(BlockFace::class.java)
        
        for ((face, startNode) in connectedNodes[networkType]!!) {
            val exploredNodes = HashSet<NetworkNode>()
            val connectedNodeFaces = HashSet<Entry<BlockFace, NetworkNode>>()
            
            var unexploredNodes = ArrayList<Entry<BlockFace, NetworkNode>>(1)
            unexploredNodes.add(AbstractMap.SimpleEntry(face, startNode))
            
            while (unexploredNodes.size != 0) { // loop until all nodes are explored
                val newUnexploredNodes = ArrayList<Entry<BlockFace, NetworkNode>>(6)
                
                unexploredNodes.forEach { unexploredEntry ->
                    val nodeToExplore = unexploredEntry.value
                    
                    if (nodeToExplore is NetworkBridge) {
                        for (connectedEntry in nodeToExplore.connectedNodes[networkType]!!) {
                            val node = connectedEntry.value
                            if (node == this) continue
                            
                            if (exploredNodes.contains(node)) {
                                connectedNodeFaces += connectedEntry
                            } else {
                                newUnexploredNodes += connectedEntry
                                exploredNodes += node
                            }
                        }
                    }
                    // node is now explored
                    connectedNodeFaces += unexploredEntry
                }
                unexploredNodes = newUnexploredNodes
            }
            // all nodes that are connected to this bridge in this direction
            networkedNodes[face] = connectedNodeFaces
        }
        
        return networkedNodes
    }
    
}