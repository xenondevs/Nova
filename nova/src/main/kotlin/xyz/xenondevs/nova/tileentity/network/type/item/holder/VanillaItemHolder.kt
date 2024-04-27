package xyz.xenondevs.nova.tileentity.network.type.item.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.commons.collections.enumSet
import xyz.xenondevs.nova.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.type.item.ItemFilter
import xyz.xenondevs.nova.tileentity.network.type.item.holder.DefaultItemHolder.Companion.DEFAULT_CHANNELS
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.util.CUBE_FACES

internal abstract class VanillaItemHolder(
    final override val compound: Compound
) : ItemHolder {
    
    override val mergedInventory: NetworkedInventory? = null
    
    override val connectionConfig: MutableMap<BlockFace, NetworkConnectionType> =
        compound["connectionConfig"] ?: CUBE_FACES.associateWithTo(enumMap()) { NetworkConnectionType.INSERT }
    
    override val allowedFaces: Set<BlockFace>
        get() = connectionConfig.mapNotNullTo(enumSet()) { (face, type) ->
            if (type != NetworkConnectionType.NONE) face else null
        }
    
    override val containers: Map<NetworkedInventory, NetworkConnectionType> by lazy {
        containerConfig.entries.associate { (_, inv) -> inv to NetworkConnectionType.BUFFER }
    }
    
    override val insertFilters: MutableMap<BlockFace, ItemFilter> =
        compound["insertFilters"] ?: enumMap()
    
    override val extractFilters: MutableMap<BlockFace, ItemFilter> =
        compound["extractFilters"] ?: enumMap()
    
    override val insertPriorities: MutableMap<BlockFace, Int> =
        compound["insertPriorities"] ?: DefaultItemHolder.DEFAULT_PRIORITIES()
    
    override val extractPriorities: MutableMap<BlockFace, Int> =
        compound["extractPriorities"] ?: DefaultItemHolder.DEFAULT_PRIORITIES()
    
    override val channels: MutableMap<BlockFace, Int> =
        compound["channels"] ?: DEFAULT_CHANNELS()
    
    override fun saveData() {
        compound["connectionConfig"] = connectionConfig
        compound["insertFilters"] = insertFilters
        compound["extractFilters"] = extractFilters
        compound["insertPriorities"] = insertPriorities
        compound["extractPriorities"] = extractPriorities
        compound["channels"] = channels
    }
    
}

internal class StaticVanillaItemHolder(
    compound: Compound,
    override val containerConfig: MutableMap<BlockFace, NetworkedInventory>
) : VanillaItemHolder(compound)

internal class DynamicVanillaItemHolder(
    compound: Compound,
    val inventoriesGetter: () -> MutableMap<BlockFace, NetworkedInventory>,
    val allowedConnectionTypesGetter: () -> Map<NetworkedInventory, NetworkConnectionType>
) : VanillaItemHolder(compound) {
    
    override val containerConfig: MutableMap<BlockFace, NetworkedInventory>
        get() = inventoriesGetter()
    
    override val containers: Map<NetworkedInventory, NetworkConnectionType>
        get() = allowedConnectionTypesGetter()
    
}
