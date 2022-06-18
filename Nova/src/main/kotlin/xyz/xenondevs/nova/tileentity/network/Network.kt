package xyz.xenondevs.nova.tileentity.network

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.config.Reloadable
import xyz.xenondevs.nova.tileentity.network.NetworkTypeRegistry.register
import xyz.xenondevs.nova.tileentity.network.energy.EnergyNetwork
import xyz.xenondevs.nova.tileentity.network.fluid.FluidNetwork
import xyz.xenondevs.nova.tileentity.network.item.ItemNetwork
import java.util.*

interface Network : Reloadable {
    
    /**
     * The [NetworkType] this network implements.
     */
    val type: NetworkType
    
    /**
     * The [Network's][Network] UUID
     */
    val uuid: UUID
    
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
     * Adds all [nodes] to this network. The pair consists of the [NetworkNode] and
     * the attached face, which can be null for [NetworkBridges][NetworkBridge].
     */
    fun addAll(nodes: Iterable<Pair<BlockFace?, NetworkNode>>)
    
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
    
    /**
     * Removes all [NetworkNodes][NetworkNode] from this [Network].
     */
    fun removeAll(nodes: List<NetworkNode>)
    
}

object NetworkTypeRegistry {
    
    private val _types = HashMap<String, NetworkType>()
    val types: List<NetworkType>
        get() = _types.values.toList()
    
    fun register(addon: Addon, name: String, networkConstructor: (UUID) -> Network): NetworkType {
        val id = NamespacedId.of(name, addon.description.id)
        val type = NetworkType(id, networkConstructor)
        _types[id.toString()] = type
        return type
    }
    
    internal fun register(name: String, networkConstructor: (UUID) -> Network): NetworkType {
        val id = NamespacedId.of(name, "nova")
        val type = NetworkType(id, networkConstructor)
        _types[id.toString()] = type
        return type
    }
    
    fun of(id: String): NetworkType? = _types[id]
    
    fun of(id: NamespacedId): NetworkType? = _types[id.toString()]
    
}

class NetworkType internal constructor(val id: NamespacedId, val networkConstructor: (UUID) -> Network) {
    
    override fun toString(): String {
        return id.toString()
    }
    
    override fun equals(other: Any?): Boolean {
        return other is NetworkType && id == other.id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    companion object {
        val ENERGY = register("energy", ::EnergyNetwork)
        val ITEMS = register("item", ::ItemNetwork)
        val FLUID = register("fluid", ::FluidNetwork)
    }
    
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
