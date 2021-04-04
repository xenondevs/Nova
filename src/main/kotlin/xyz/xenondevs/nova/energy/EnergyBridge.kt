package xyz.xenondevs.nova.energy

import org.bukkit.block.BlockFace
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.collections.Map.Entry

/**
 * Finds the nodes directly connected to this [EnergyBridge] on each side
 * of the block.
 */
fun EnergyBridge.findConnectedNodes(): Map<BlockFace, EnergyNode> =
    getNearbyNodes().filterTo(EnumMap(BlockFace::class.java)) { (face, node) -> node.getNetwork(face.oppositeFace) == network }

/**
 * Gets a map of attached [EnergyNode]s for each side of this [EnergyBridge].
 * The content of the maps might be the same in cases where multiple
 * [EnergyBridge]s are used to connect the same [EnergyNode]s.
 */
fun EnergyBridge.getNetworkedNodes(): Map<BlockFace, Set<Entry<BlockFace, EnergyNode>>> {
    val networkedNodes = EnumMap<BlockFace, Set<Entry<BlockFace, EnergyNode>>>(BlockFace::class.java)
    
    for ((face, startNode) in connectedNodes) {
        val exploredNodes = HashSet<EnergyNode>()
        val connectedNodeFaces = HashSet<Entry<BlockFace, EnergyNode>>()
        
        var unexploredNodes = ArrayList<Entry<BlockFace, EnergyNode>>(1)
        unexploredNodes.add(AbstractMap.SimpleEntry(face, startNode))
        
        while (unexploredNodes.size != 0) { // loop until all nodes are explored
            val newUnexploredNodes = ArrayList<Entry<BlockFace, EnergyNode>>(6)
            
            unexploredNodes.forEach { unexploredEntry ->
                val nodeToExplore = unexploredEntry.value
                
                if (nodeToExplore is EnergyBridge) {
                    for (connectedEntry in nodeToExplore.connectedNodes) {
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
 * Basically cables. Transfer energy inside an [EnergyNetwork].
 */
interface EnergyBridge : EnergyNode {
    
    /**
     * The [EnergyNetwork] this [EnergyBridge] routes energy trough.
     */
    var network: EnergyNetwork?
    
    /**
     * How much energy can be transferred per tick. If there are different
     * types of [EnergyBridge]s inside an [EnergyNetwork], the transfer of the
     * whole network is equal to the smallest one.
     */
    val transferRate: Int
    
    /**
     * Block faces that are allowed to transmit energy in
     * an [EnergyNetwork].
     */
    val bridgeFaces: Set<BlockFace>
    
    /**
     * Caches the directly connected nodes.
     * Should be updated when handleNetworkUpdate is called.
     */
    val connectedNodes: Map<BlockFace, EnergyNode>
    
    /**
     * Called when another [EnergyNode] has ben placed
     * or broken right next to this node.
     *
     * Not called when [EnergyNode]s nearby get unloaded.
     *
     * Useful for updating the model of a cable to connect
     * to additional sides.
     */
    fun handleNetworkUpdate()
    
}