package xyz.xenondevs.nova.tileentity.network

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.tileentity.network.energy.EnergyNetwork
import xyz.xenondevs.nova.tileentity.network.fluid.FluidNetwork
import xyz.xenondevs.nova.tileentity.network.item.ItemNetwork

interface Network {
    
    /**
     * The [NetworkType] this network implements.
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
     * Checks if the configuration of this network makes sense or
     * if it would never do anything and should be removed.
     */
    fun isValid(): Boolean
    
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

enum class NetworkType(val networkConstructor: () -> Network) {
    
    /**
     * Transfers Energy
     */
    ENERGY(::EnergyNetwork),
    
    /**
     * Transfers Items
     */
    ITEMS(::ItemNetwork),
    
    /**
     * Transfers Fluids
     */
    FLUID(::FluidNetwork)
    
}

enum class NetworkConnectionType(val insert: Boolean, val extract: Boolean, included: ArrayList<NetworkConnectionType>) {
    
    NONE(false, false, arrayListOf()),
    INSERT(true, false, arrayListOf(NONE)),
    EXTRACT(false, true, arrayListOf(NONE)),
    BUFFER(true, true, arrayListOf(NONE, INSERT, EXTRACT));
    
    val included: List<NetworkConnectionType> = included.also { it.add(this) }
    val insertExtract = insert to extract
    
    companion object {
        
        fun of(insert: Boolean, extract: Boolean) = values().first { it.insert == insert && it.extract == extract }
        
    }
    
}

class NetworkException(message: String) : RuntimeException(message)
