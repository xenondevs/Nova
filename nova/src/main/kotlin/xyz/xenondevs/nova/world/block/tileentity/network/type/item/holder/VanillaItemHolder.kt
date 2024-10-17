package xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.entry
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.commons.collections.enumSet
import xyz.xenondevs.commons.collections.toEnumMap
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.orElseNew
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.ItemFilter
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.DefaultItemHolder.Companion.DEFAULT_CHANNELS
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.NetworkedInventory

internal abstract class VanillaItemHolder(
    compound: Provider<Compound>,
    defaultConnectionConfig: () -> Map<BlockFace, NetworkConnectionType>
) : ItemHolder {
    
    override val mergedInventory: NetworkedInventory? = null
    
    override val connectionConfig: MutableMap<BlockFace, NetworkConnectionType>
        by compound.entry<MutableMap<BlockFace, NetworkConnectionType>>("connectionConfig")
            .orElseNew { defaultConnectionConfig().toEnumMap() }
    
    override val allowedFaces: Set<BlockFace>
        get() = connectionConfig.mapNotNullTo(enumSet()) { (face, type) ->
            if (type != NetworkConnectionType.NONE) face else null
        }
    
    override val containers: Map<NetworkedInventory, NetworkConnectionType> by lazy {
        containerConfig.entries.associate { (_, inv) -> inv to NetworkConnectionType.BUFFER }
    }
    
    override val insertFilters: MutableMap<BlockFace, ItemFilter<*>>
        by compound.entry<MutableMap<BlockFace, ItemFilter<*>>>("insertFilters")
            .orElseNew(::enumMap)
    
    override val extractFilters: MutableMap<BlockFace, ItemFilter<*>>
        by compound.entry<MutableMap<BlockFace, ItemFilter<*>>>("extractFilters")
            .orElseNew(::enumMap)
    
    override val insertPriorities: MutableMap<BlockFace, Int>
        by compound.entry<MutableMap<BlockFace, Int>>("insertPriorities")
            .orElseNew(DefaultItemHolder.DEFAULT_PRIORITIES)
    
    override val extractPriorities: MutableMap<BlockFace, Int>
        by compound.entry<MutableMap<BlockFace, Int>>("extractPriorities")
            .orElseNew(DefaultItemHolder.DEFAULT_PRIORITIES)
    
    override val channels: MutableMap<BlockFace, Int>
        by compound.entry<MutableMap<BlockFace, Int>>("channels")
            .orElseNew(DEFAULT_CHANNELS)
    
}

internal class StaticVanillaItemHolder(
    compound: Provider<Compound>,
    override val containerConfig: MutableMap<BlockFace, NetworkedInventory>,
    defaultConnectionConfig: () -> Map<BlockFace, NetworkConnectionType> = { CUBE_FACES.associate { it to NetworkConnectionType.BUFFER } }
) : VanillaItemHolder(compound, defaultConnectionConfig) {
    
    override val blockedFaces: Set<BlockFace>
        get() = emptySet()
    
}

internal class DynamicVanillaItemHolder(
    compound: Provider<Compound>,
    val inventoriesGetter: () -> MutableMap<BlockFace, NetworkedInventory>,
    val allowedConnectionTypesGetter: () -> Map<NetworkedInventory, NetworkConnectionType>,
    defaultConnectionConfig: () -> Map<BlockFace, NetworkConnectionType> = { CUBE_FACES.associate { it to NetworkConnectionType.BUFFER } }
) : VanillaItemHolder(compound, defaultConnectionConfig) {
    
    override val blockedFaces: Set<BlockFace>
        get() = emptySet()
    
    override val containerConfig: MutableMap<BlockFace, NetworkedInventory>
        get() = inventoriesGetter()
    
    override val containers: Map<NetworkedInventory, NetworkConnectionType>
        get() = allowedConnectionTypesGetter()
    
}
