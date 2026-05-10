package xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder

import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.entry
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.orElse
import xyz.xenondevs.nova.util.CubeFaceMap
import xyz.xenondevs.nova.util.CubeFaceSet
import xyz.xenondevs.nova.world.block.tileentity.network.node.DefaultContainerEndPointDataHolder.Companion.DEFAULT_CHANNEL_CONFIG
import xyz.xenondevs.nova.world.block.tileentity.network.node.DefaultContainerEndPointDataHolder.Companion.DEFAULT_PRIORITIES
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.ItemFilter
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.NetworkedInventory

private val ALL_BUFFER = CubeFaceMap(NetworkConnectionType.BUFFER)

internal abstract class VanillaItemHolder(
    compound: Provider<Compound>,
    defaultConnectionConfig: CubeFaceMap<NetworkConnectionType> = ALL_BUFFER
) : ItemHolder {
    
    override val mergedInventory: NetworkedInventory? = null
    
    override var connectionConfig: CubeFaceMap<NetworkConnectionType>
        by compound.entry<CubeFaceMap<NetworkConnectionType>>("connectionConfig")
            .orElse(defaultConnectionConfig)
    
    override val containers: Map<NetworkedInventory, NetworkConnectionType> by lazy {
        containerConfig.values.associateWith { NetworkConnectionType.BUFFER }
    }
    
    override var insertFilters: CubeFaceMap<ItemFilter<*>?>
        by compound.entry<CubeFaceMap<ItemFilter<*>?>>("insertFilters")
            .orElse(CubeFaceMap.NULL)
    
    override var extractFilters:  CubeFaceMap<ItemFilter<*>?>
        by compound.entry<CubeFaceMap<ItemFilter<*>?>>("extractFilters")
            .orElse(CubeFaceMap.NULL)
    
    override var insertPriorities: CubeFaceMap<Int>
        by compound.entry<CubeFaceMap<Int>>("insertPriorities")
            .orElse(DEFAULT_PRIORITIES)
    
    override var extractPriorities: CubeFaceMap<Int>
        by compound.entry<CubeFaceMap<Int>>("extractPriorities")
            .orElse(DEFAULT_PRIORITIES)
    
    override var channels: CubeFaceMap<Int>
        by compound.entry<CubeFaceMap<Int>>("channels")
            .orElse(DEFAULT_CHANNEL_CONFIG)
    
}

internal class StaticVanillaItemHolder(
    compound: Provider<Compound>,
    override var containerConfig: CubeFaceMap<NetworkedInventory?>,
    defaultConnectionConfig: CubeFaceMap<NetworkConnectionType> = ALL_BUFFER
) : VanillaItemHolder(compound, defaultConnectionConfig) {
    
    override val blockedFaces: CubeFaceSet
        get() = CubeFaceSet.NONE
    
}

internal class DynamicVanillaItemHolder(
    compound: Provider<Compound>,
    val inventoriesGetter: () -> CubeFaceMap<NetworkedInventory>,
    val allowedConnectionTypesGetter: () -> Map<NetworkedInventory, NetworkConnectionType>,
    defaultConnectionConfig: CubeFaceMap<NetworkConnectionType> = ALL_BUFFER
) : VanillaItemHolder(compound, defaultConnectionConfig) {
    
    override val blockedFaces: CubeFaceSet
        get() = CubeFaceSet.NONE
    
    override var containerConfig: CubeFaceMap<NetworkedInventory?>
        get() = inventoriesGetter()
        set(_) {}
    
    override val containers: Map<NetworkedInventory, NetworkConnectionType>
        get() = allowedConnectionTypesGetter()
    
}
