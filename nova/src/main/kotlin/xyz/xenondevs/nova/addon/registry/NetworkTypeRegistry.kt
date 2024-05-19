package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.tileentity.network.Network
import xyz.xenondevs.nova.tileentity.network.NetworkData
import xyz.xenondevs.nova.tileentity.network.NetworkGroup
import xyz.xenondevs.nova.tileentity.network.NetworkGroupData
import xyz.xenondevs.nova.tileentity.network.node.EndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.type.NetworkType
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.set
import kotlin.reflect.KClass

interface NetworkTypeRegistry : AddonGetter {
    
    fun <T : Network<T>> registerNetworkType(
        name: String,
        networkConstructor: (NetworkData<T>) -> T,
        groupConstructor: (NetworkGroupData<T>) -> NetworkGroup<T>,
        tickDelay: Provider<Int>,
        vararg holderTypes: KClass<out EndPointDataHolder>
    ): NetworkType<T> {
        val id = ResourceLocation(addon, name)
        val networkType = NetworkType(
            id, 
            networkConstructor, groupConstructor,
            tickDelay, 
            holderTypes.toHashSet()
        )
        
        NovaRegistries.NETWORK_TYPE[id] = networkType
        return networkType
    }
    
}