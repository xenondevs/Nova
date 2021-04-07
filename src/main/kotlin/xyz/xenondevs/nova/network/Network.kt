package xyz.xenondevs.nova.network

import org.bukkit.block.BlockFace

interface Network {
    
    /**
     * What [NetworkType] this [Network] is.
     */
    val type: NetworkType
    
    /**
     * A set of [NetworkNode]s that are connected to this [Network].
     */
    val nodes: Set<NetworkNode>
    
    /**
     * Checks if there are any nodes in this network.
     * Should always return the same as nodes.isEmpty()
     */
    fun isEmpty(): Boolean
    
    /**
     * Called every tick
     */
    fun handleTick()
    
    /**
     * Adds all [NetworkNode]s of the given [Network] to this [Network].
     * Should only be called for [Network]s of the same type.
     */
    fun addAll(network: Network)
    
    /**
     * Adds an [NetworkEndPoint] to this [Network].
     * The [BlockFace] specifies which side of this [NetworkEndPoint]
     * was connected to the network.
     */
    fun addEndPoint(endPoint: NetworkEndPoint, face: BlockFace)
    
    /**
     * Adds a [NetworkBridge] to this [Network].
     */
    fun addBridge(bridge: NetworkBridge)
    
    
    /**
     * Removes a [NetworkNode] from this [Network].
     */
    fun removeNode(node: NetworkNode)
    
}

