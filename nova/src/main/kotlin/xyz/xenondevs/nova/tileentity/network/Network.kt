package xyz.xenondevs.nova.tileentity.network

import net.minecraft.resources.ResourceLocation
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.tileentity.network.energy.EnergyNetwork
import xyz.xenondevs.nova.tileentity.network.fluid.FluidNetwork
import xyz.xenondevs.nova.tileentity.network.item.ItemNetwork
import xyz.xenondevs.nova.util.set
import java.util.*

interface Network {
    
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

private typealias NetworkConstructor = (UUID, Boolean) -> Network

class NetworkType internal constructor(val id: ResourceLocation, val networkConstructor: NetworkConstructor) {
    
    override fun toString(): String {
        return id.toString()
    }
    
    override fun equals(other: Any?): Boolean {
        return other is NetworkType && id == other.id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
}

@InternalInit(stage = InternalInitStage.PRE_WORLD)
object DefaultNetworkTypes {
    
    val ENERGY = register("energy") { uuid, _ -> EnergyNetwork(uuid) }
    val ITEMS = register("item", ::ItemNetwork)
    val FLUID = register("fluid") { uuid, _ -> FluidNetwork(uuid) }
    
    private fun register(name: String, networkConstructor: NetworkConstructor): NetworkType {
        val id = ResourceLocation("nova", name)
        val type = NetworkType(id, networkConstructor)
        NovaRegistries.NETWORK_TYPE[id] = type
        return type
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
        
        fun of(insert: Boolean, extract: Boolean): NetworkConnectionType =
            entries.first { it.insert == insert && it.extract == extract }
        
        fun of(types: Iterable<NetworkConnectionType>): NetworkConnectionType {
            var insert = false
            var extract = false
            types.forEach {
                insert = insert or it.insert
                extract = extract or it.extract
            }
            
            return of(insert, extract)
        }
        
    }
    
}

class NetworkException(message: String) : RuntimeException(message)
