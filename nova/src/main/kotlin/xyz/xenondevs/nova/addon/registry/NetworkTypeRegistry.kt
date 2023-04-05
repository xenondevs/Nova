package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.tileentity.network.Network
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.set
import java.util.*

private typealias NetworkConstructor = (UUID, Boolean) -> Network

interface NetworkTypeRegistry: AddonGetter {
    
    fun registerNetworkType(name: String, networkConstructor: NetworkConstructor): NetworkType {
        val id = ResourceLocation(addon, name)
        val networkType = NetworkType(id, networkConstructor)
        
        NovaRegistries.NETWORK_TYPE[id] = networkType
        return networkType
    }
    
}