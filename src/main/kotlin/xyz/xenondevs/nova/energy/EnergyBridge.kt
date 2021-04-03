package xyz.xenondevs.nova.energy

import org.bukkit.block.BlockFace
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.collections.Map.Entry

/**
 * Gets the nodes directly connected to this [EnergyBridge] on each side
 * of the block.
 */
fun EnergyBridge.getConnectedNodes(): Map<BlockFace, NetworkNode> =
    getNearbyNodes().filterTo(EnumMap(BlockFace::class.java)) { (face, node) -> node.getNetwork(face.oppositeFace) == network }

/**
 * Gets a map of attached [NetworkNode]s for each side of this [EnergyBridge].
 * The content of the maps might be the same in cases where multiple
 * [EnergyBridge]s are used to connect the same [NetworkNode]s.
 */
fun EnergyBridge.getNetworkedNodes(): Map<BlockFace, Set<Entry<BlockFace, NetworkNode>>> {
    // TODO: optimize, this crashes the server if there are many cables that all connect to each other
    
    // using Map.Entry instead of Pair for performance so no new Pairs need to be created
    val networkedNodes = EnumMap<BlockFace, Set<Entry<BlockFace, NetworkNode>>>(BlockFace::class.java)
    
    getConnectedNodes().forEach { (face, startNode) ->
        val exploredNodes = HashSet<Entry<BlockFace, NetworkNode>>()
        
        var unexploredNodes = ArrayList<Entry<BlockFace, NetworkNode>>(1) // not using CopyOnWriteArrayList for performance
        unexploredNodes.add(AbstractMap.SimpleEntry(face, startNode))
        
        while (unexploredNodes.size != 0) { // loop until all nodes are explored
            val newUnexploredNodes = ArrayList<Entry<BlockFace, NetworkNode>>(6)
            
            unexploredNodes.forEach { unexploredEntry ->
                val nodeToExplore = unexploredEntry.value
                
                if (nodeToExplore is EnergyBridge) {
                    nodeToExplore.getConnectedNodes().forEach { connectedEntry ->
                        if (!exploredNodes.contains(connectedEntry)) newUnexploredNodes += connectedEntry
                    }
                }
                
                // node is now explored
                exploredNodes += unexploredEntry
            }
            
            unexploredNodes = newUnexploredNodes
        }
        
        // all nodes that are connected to this bridge in this direction
        networkedNodes[face] = exploredNodes
    }
    
    return networkedNodes
}

/**
 * Basically cables. Transfer energy inside an [EnergyNetwork].
 */
interface EnergyBridge : NetworkNode {
    
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
     * Called when another [NetworkNode] has ben placed
     * or broken right next to this node.
     *
     * Not called when [NetworkNode]s nearby get unloaded.
     *
     * Useful for updating the model of a cable to connect
     * to additional sides.
     */
    fun handleNetworkUpdate()
    
}