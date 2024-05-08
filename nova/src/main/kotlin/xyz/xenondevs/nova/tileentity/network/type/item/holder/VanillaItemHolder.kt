package xyz.xenondevs.nova.tileentity.network.type.item.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.provider.entry
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.commons.collections.enumSet
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.mutable.orElseLazily
import xyz.xenondevs.nova.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.type.item.ItemFilter
import xyz.xenondevs.nova.tileentity.network.type.item.holder.DefaultItemHolder.Companion.DEFAULT_CHANNELS
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.util.CUBE_FACES

internal abstract class VanillaItemHolder(
    compound: Provider<Compound>
) : ItemHolder {
    
    override val mergedInventory: NetworkedInventory? = null
    
    override val connectionConfig: MutableMap<BlockFace, NetworkConnectionType> by
        compound.entry<MutableMap<BlockFace, NetworkConnectionType>>("connectionConfig")
            .orElseLazily { CUBE_FACES.associateWithTo(enumMap()) { NetworkConnectionType.INSERT } }
    
    override val allowedFaces: Set<BlockFace>
        get() = connectionConfig.mapNotNullTo(enumSet()) { (face, type) ->
            if (type != NetworkConnectionType.NONE) face else null
        }
    
    override val containers: Map<NetworkedInventory, NetworkConnectionType> by lazy {
        containerConfig.entries.associate { (_, inv) -> inv to NetworkConnectionType.BUFFER }
    }
    
    override val insertFilters: MutableMap<BlockFace, ItemFilter> by
        compound.entry<MutableMap<BlockFace, ItemFilter>>("insertFilters")
            .orElseLazily(::enumMap)
    
    override val extractFilters: MutableMap<BlockFace, ItemFilter> by
        compound.entry<MutableMap<BlockFace, ItemFilter>>("extractFilters")
            .orElseLazily(::enumMap)
    
    override val insertPriorities: MutableMap<BlockFace, Int> by
        compound.entry<MutableMap<BlockFace, Int>>("insertPriorities")
            .orElseLazily(DefaultItemHolder.DEFAULT_PRIORITIES)
    
    override val extractPriorities: MutableMap<BlockFace, Int> by
        compound.entry<MutableMap<BlockFace, Int>>("extractPriorities")
            .orElseLazily(DefaultItemHolder.DEFAULT_PRIORITIES)
    
    override val channels: MutableMap<BlockFace, Int> by
        compound.entry<MutableMap<BlockFace, Int>>("channels")
            .orElseLazily(DEFAULT_CHANNELS)
    
}

internal class StaticVanillaItemHolder(
    compound: Provider<Compound>,
    override val containerConfig: MutableMap<BlockFace, NetworkedInventory>
) : VanillaItemHolder(compound)

internal class DynamicVanillaItemHolder(
    compound: Provider<Compound>,
    val inventoriesGetter: () -> MutableMap<BlockFace, NetworkedInventory>,
    val allowedConnectionTypesGetter: () -> Map<NetworkedInventory, NetworkConnectionType>
) : VanillaItemHolder(compound) {
    
    override val containerConfig: MutableMap<BlockFace, NetworkedInventory>
        get() = inventoriesGetter()
    
    override val containers: Map<NetworkedInventory, NetworkConnectionType>
        get() = allowedConnectionTypesGetter()
    
}
