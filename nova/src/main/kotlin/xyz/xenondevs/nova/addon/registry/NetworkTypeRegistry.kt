package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.addon.REGISTRIES_DEPRECATION
import xyz.xenondevs.nova.world.block.tileentity.network.Network
import xyz.xenondevs.nova.world.block.tileentity.network.node.EndPointDataHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.LocalValidator
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConstructor
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkGroupConstructor
import kotlin.reflect.KClass

@Suppress("DEPRECATION")
@Deprecated(REGISTRIES_DEPRECATION)
interface NetworkTypeRegistry : AddonGetter {
    
    @Deprecated(REGISTRIES_DEPRECATION)
    fun <T : Network<T>> registerNetworkType(
        name: String,
        createNetwork: NetworkConstructor<T>,
        createGroup: NetworkGroupConstructor<T>,
        validateLocal: LocalValidator,
        tickDelay: Provider<Int>,
        vararg holderTypes: KClass<out EndPointDataHolder>
    ) = addon.registerNetworkType(name, createNetwork, createGroup, validateLocal, tickDelay, *holderTypes)
    
}