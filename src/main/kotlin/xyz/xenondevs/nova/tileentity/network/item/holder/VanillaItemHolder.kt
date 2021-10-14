package xyz.xenondevs.nova.tileentity.network.item.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.tileentity.network.item.ItemConnectionType
import xyz.xenondevs.nova.tileentity.network.item.ItemFilter
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.tileentity.vanilla.ItemStorageVanillaTileEntity
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.emptyEnumMap
import java.util.*

abstract class VanillaItemHolder(
    final override val endPoint: ItemStorageVanillaTileEntity
) : ItemHolder {
    
    override val itemConfig: MutableMap<BlockFace, ItemConnectionType> =
        endPoint.retrieveDoubleEnumMap("itemConfig") { CUBE_FACES.associateWithTo(EnumMap(BlockFace::class.java)) { ItemConnectionType.BUFFER } }
    
    override val allowedConnectionTypes: Map<NetworkedInventory, ItemConnectionType> by lazy {
        inventories.entries.associate { (_, inv) -> inv to ItemConnectionType.BUFFER }
    }
    
    override val insertFilters: MutableMap<BlockFace, ItemFilter> =
        endPoint.retrieveEnumMap<BlockFace, CompoundElement>("insertFilters") { emptyEnumMap() }
            .mapValuesTo(emptyEnumMap()) { ItemFilter(it.value) }
    
    override val extractFilters: MutableMap<BlockFace, ItemFilter> =
        endPoint.retrieveEnumMap<BlockFace, CompoundElement>("extractFilters") { emptyEnumMap() }
            .mapValuesTo(emptyEnumMap()) { ItemFilter(it.value) }
    
    override val insertPriorities: MutableMap<BlockFace, Int> =
        endPoint.retrieveEnumMap("insertPriorities") { CUBE_FACES.associateWithTo(emptyEnumMap()) { 50 } }
    
    override val extractPriorities: MutableMap<BlockFace, Int> =
        endPoint.retrieveEnumMap("extractPriorities") { CUBE_FACES.associateWithTo(emptyEnumMap()) { 50 } }
    
    override val channels: MutableMap<BlockFace, Int> =
        endPoint.retrieveEnumMap("channels") { CUBE_FACES.associateWithTo(emptyEnumMap()) { 0 } }
    
    override fun saveData() {
        endPoint.storeEnumMap("itemConfig", itemConfig)
        endPoint.storeEnumMap("insertFilters", insertFilters) { it.compound }
        endPoint.storeEnumMap("extractFilters", extractFilters) { it.compound }
        endPoint.storeEnumMap("insertPriorities", insertPriorities)
        endPoint.storeEnumMap("extractPriorities", extractPriorities)
        endPoint.storeEnumMap("channels", channels)
    }
    
}

class StaticVanillaItemHolder(
    endPoint: ItemStorageVanillaTileEntity,
    override val inventories: MutableMap<BlockFace, NetworkedInventory>
) : VanillaItemHolder(endPoint)

class DynamicVanillaItemHolder(
    endPoint: ItemStorageVanillaTileEntity,
    val inventoriesGetter: () -> MutableMap<BlockFace, NetworkedInventory>
) : VanillaItemHolder(endPoint) {
    
    override val inventories: MutableMap<BlockFace, NetworkedInventory>
        get() = inventoriesGetter()
    
}
