package xyz.xenondevs.nova.tileentity.network

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
     * A set of [NetworkTypes][NetworkType] that are supported by this bridge.
     */
    val supportedNetworkTypes: Set<NetworkType>
    
    /**
     * An identifier to prevent bridges of different tiers from connecting to each other.
     */
    val typeId: Int
    
    /**
     * Called when another [NetworkNode] has been placed
     * or broken right next to this node.
     *
     * Not called when [NetworkNode]s nearby get unloaded.
     *
     * Useful for updating the model of a cable to connect
     * to additional sides.
     */
    fun handleNetworkUpdate()
    
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
    
    /**
     * Checks if this bridge is able to connect to its neighboring bridge.
     */
    fun canConnect(other: NetworkBridge, requestedType: NetworkType, face: BlockFace): Boolean {
        return typeId == other.typeId
            && bridgeFaces.contains(face)
            && other.bridgeFaces.contains(face.oppositeFace)
            && other.supportedNetworkTypes.contains(requestedType)
    }
    
}