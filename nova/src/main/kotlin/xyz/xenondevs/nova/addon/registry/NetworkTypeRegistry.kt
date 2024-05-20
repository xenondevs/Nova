package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.tileentity.network.Network
import xyz.xenondevs.nova.tileentity.network.node.EndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.type.LocalValidator
import xyz.xenondevs.nova.tileentity.network.type.NetworkGroupConstructor
import xyz.xenondevs.nova.tileentity.network.type.NetworkConstructor
import xyz.xenondevs.nova.tileentity.network.type.NetworkType
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.set
import kotlin.reflect.KClass

interface NetworkTypeRegistry : AddonGetter {
    
    fun <T : Network<T>> registerNetworkType(
        name: String,
        createNetwork: NetworkConstructor<T>,
        createGroup: NetworkGroupConstructor<T>,
        validateLocal: LocalValidator,
        tickDelay: Provider<Int>,
        vararg holderTypes: KClass<out EndPointDataHolder>
    ): NetworkType<T> {
        val id = ResourceLocation(addon, name)
        val networkType = NetworkType(
            id, 
            createNetwork, createGroup, validateLocal,
            tickDelay, 
            holderTypes.toHashSet()
        )
        
        NovaRegistries.NETWORK_TYPE[id] = networkType
        return networkType
    }
    
}