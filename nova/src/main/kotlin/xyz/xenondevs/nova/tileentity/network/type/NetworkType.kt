package xyz.xenondevs.nova.tileentity.network.type

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.tileentity.network.Network
import xyz.xenondevs.nova.tileentity.network.NetworkData
import xyz.xenondevs.nova.tileentity.network.node.ContainerEndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.node.EndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.type.energy.EnergyNetwork
import xyz.xenondevs.nova.tileentity.network.type.energy.holder.EnergyHolder
import xyz.xenondevs.nova.tileentity.network.type.fluid.FluidNetwork
import xyz.xenondevs.nova.tileentity.network.type.fluid.holder.FluidHolder
import xyz.xenondevs.nova.tileentity.network.type.item.ItemNetwork
import xyz.xenondevs.nova.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.util.set
import java.util.*
import kotlin.reflect.KClass

internal typealias NetworkConstructor = (NetworkData) -> Network

/**
 * A [Network] type.
 * 
 * @param id The unique identifier of this [NetworkType].
 * @param constructor The constructor to instantiate a [Network] of this [NetworkType].
 * @param tickDelay The delay between [network ticks][Network.handleTick].
 * @param holderTypes The types of [EndPointDataHolders][EndPointDataHolder]
 * that are required for end points of this [NetworkType].
 */
class NetworkType internal constructor(
    val id: ResourceLocation,
    private val constructor: NetworkConstructor,
    tickDelay: Provider<Int>,
    val holderTypes: Set<KClass<out EndPointDataHolder>>
) {
    
    /**
     * The delay between [network ticks][Network.handleTick].
     */
    val tickDelay: Int by tickDelay
    
    /**
     * Creates a new [Network] based on the given [data].
     */
    fun create(data: NetworkData): Network =
        constructor(data)
    
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

/**
 * The default network types provided by Nova.
 */
@InternalInit(stage = InternalInitStage.PRE_WORLD)
object DefaultNetworkTypes {
    
    /**
     * The default network type responsible for distributing energy provided through [EnergyHolders][EnergyHolder].
     */
    val ENERGY = register("energy", ::EnergyNetwork, EnergyNetwork.TICK_DELAY_PROVIDER, EnergyHolder::class)
    
    /**
     * The default network type responsible for distributing items provided through [ItemHolders][ItemHolder].
     */
    val ITEM = register("item", ::ItemNetwork, ItemNetwork.TICK_DELAY_PROVIDER, ItemHolder::class)
    
    /**
     * The default network type responsible for distributing fluids provided through [FluidHolders][FluidHolder].
     */
    val FLUID = register("fluid", ::FluidNetwork, FluidNetwork.TICK_DELAY_PROVIDER, FluidHolder::class)
    
    private fun register(
        name: String,
        constructor: NetworkConstructor,
        tickDelay: Provider<Int>,
        vararg holderTypes: KClass<out EndPointDataHolder>
    ): NetworkType {
        val id = ResourceLocation("nova", name)
        val type = NetworkType(id, constructor, tickDelay, holderTypes.toHashSet())
        NovaRegistries.NETWORK_TYPE[id] = type
        return type
    }
    
}

/**
 * A connection type for [ContainerEndPointDataHolders][ContainerEndPointDataHolder].
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