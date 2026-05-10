package xyz.xenondevs.nova.world.block.tileentity.network.type

import kotlinx.serialization.Serializable
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.NovaRegistrar.registerNetworkType
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.registry.RegistryLoader
import xyz.xenondevs.nova.serialization.kotlinx.NetworkTypeEntrySerializer
import xyz.xenondevs.nova.serialization.kotlinx.NetworkTypeEntrySetSerializer
import xyz.xenondevs.nova.serialization.kotlinx.NetworkTypeSerializer
import xyz.xenondevs.nova.world.block.tileentity.network.Network
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkData
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkGroup
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkGroupData
import xyz.xenondevs.nova.world.block.tileentity.network.node.EndPointDataHolder
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.type.energy.EnergyNetwork
import xyz.xenondevs.nova.world.block.tileentity.network.type.energy.EnergyNetworkGroup
import xyz.xenondevs.nova.world.block.tileentity.network.type.energy.holder.EnergyHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.FluidNetwork
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.FluidNetworkGroup
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.holder.FluidHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.ItemNetwork
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.ItemNetworkGroup
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.ItemHolder

/**
 * Serializable type alias for `RegistryEntry.Nova<NetworkType<T>>` using [NetworkTypeEntrySerializer].
 */
typealias NetworkTypeEntry<T> = @Serializable(with = NetworkTypeEntrySerializer::class) RegistryEntry.Nova<NetworkType<T>>

/**
 * Serializable type alias for `RegistryEntrySet.Nova<NetworkType<T>>` using [NetworkTypeEntrySetSerializer].
 */
typealias NetworkTypeEntrySet<T> = @Serializable(with = NetworkTypeEntrySetSerializer::class) RegistryEntrySet.Nova<NetworkType<T>>

/**
 * Typealias for a local network validator. The lambda is called to check whether a local (a network of only two end points)
 * can be created by connecting `from` to `to` through `face`.
 */
typealias LocalNetworkValidator = (from: NetworkEndPoint, to: NetworkEndPoint, face: BlockFace) -> Boolean

/**
 * A network type. Specifies how and when to create a [Network] and the associated [NetworkGroup].
 */
@Serializable(with = NetworkTypeSerializer::class)
class NetworkType<T : Network<T>> internal constructor(
    override val entry: RegistryEntry.Nova<NetworkType<T>>,
    /**
     * The constructor to instantiate a [Network] of this type.
     */
    val createNetwork: (NetworkData<T>) -> T,
    /**
     * The constructor to instantiate a [NetworkGroup] of this type.
     */
    val createGroup: (NetworkGroupData<T>) -> NetworkGroup<T>,
    /**
     * A function that checks whether a local network can be created between two end points.
     * 
     * A local network will only be created it this validator returns `true` and at least one of the
     * end points [requests][NetworkEndPoint.requestsLocalNetwork] a local network.
     */
    val validateLocal: LocalNetworkValidator,
    /**
     * A function that gets the required [data holders][EndPointDataHolder] from an [end point's holders][NetworkEndPoint.holders].
     * Only if all returned [data holders][EndPointDataHolder] [allow a connection at a face][EndPointDataHolder.allowedFaces]
     * a network will be created there. Can return `null` if the given end point does not support this network type.
     */
    val extractHolders: (NetworkEndPoint) -> List<EndPointDataHolder>?,
    /**
     * The delay between [network ticks][NetworkGroup.tick].
     */
    val tickDelay: Int
) : NovaRegistryElement<NetworkType<T>> {
    
    override fun toString(): String = key.toString()
    override fun hashCode(): Int = key.hashCode()
    override fun equals(other: Any?): Boolean = other is NetworkType<*> && key == other.key
    
}

/**
 * The default network types provided by Nova.
 */
@InternalInit(
    stage = InternalInitStage.PRE_WORLD,
    runBefore = [RegistryLoader::class]
)
object DefaultNetworkTypes {
    
    /**
     * The default network type responsible for distributing energy provided through [EnergyHolders][EnergyHolder].
     */
    val ENERGY = registerNetworkType(
        "energy",
        ::EnergyNetwork,
        ::EnergyNetworkGroup,
        EnergyNetwork::validateLocal,
        EnergyNetwork::extractHolders,
        EnergyNetwork.TICK_DELAY_PROVIDER.get(),
    )
    
    /**
     * The default network type responsible for distributing items provided through [ItemHolders][ItemHolder].
     */
    val ITEM = registerNetworkType(
        "item",
        ::ItemNetwork,
        ::ItemNetworkGroup,
        ItemNetwork::validateLocal,
        ItemNetwork::extractHolders,
        ItemNetwork.TICK_DELAY_PROVIDER.get(),
    )
    
    /**
     * The default network type responsible for distributing fluids provided through [FluidHolders][FluidHolder].
     */
    val FLUID = registerNetworkType(
        "fluid",
        ::FluidNetwork,
        ::FluidNetworkGroup,
        FluidNetwork::validateLocal,
        FluidNetwork::extractHolders,
        FluidNetwork.TICK_DELAY_PROVIDER.get(),
    )
    
}

/**
 * A connection type for end point data holders.
 *
 * @param insert Whether this connection type allows inserting.
 * @param extract Whether this connection type allows extracting.
 * @param supertypes The supertypes of this connection type.
 */
enum class NetworkConnectionType(val insert: Boolean, val extract: Boolean, supertypes: List<NetworkConnectionType>) {
    
    /**
     * No connection.
     */
    NONE(false, false, emptyList()),
    
    /**
     * An insert-only connection.
     */
    INSERT(true, false, listOf(NONE)),
    
    /**
     * An extract-only connection.
     */
    EXTRACT(false, true, listOf(NONE)),
    
    /**
     * A combination of [INSERT] and [EXTRACT].
     */
    BUFFER(true, true, listOf(NONE, INSERT, EXTRACT));
    
    /**
     * The supertypes of this connection type, including itself.
     */
    val supertypes: List<NetworkConnectionType> = supertypes + this
    
    companion object {
        
        /**
         * Retrieves the [NetworkConnectionType] that fits the given [insert] and [extract] values.
         */
        fun of(insert: Boolean, extract: Boolean): NetworkConnectionType =
            when {
                insert && extract -> BUFFER
                insert -> INSERT
                extract -> EXTRACT
                else -> NONE
            }
        
        /**
         * Retrieves the [NetworkConnectionType] that fits the given [types].
         */
        fun of(types: Iterable<NetworkConnectionType>): NetworkConnectionType {
            var insert = false
            var extract = false
            for (type in types) {
                insert = insert || type.insert
                extract = extract || type.extract
            }
            
            return of(insert, extract)
        }
        
    }
    
}