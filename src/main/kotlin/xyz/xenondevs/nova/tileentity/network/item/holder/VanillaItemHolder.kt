package xyz.xenondevs.nova.tileentity.network.item.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.item.ItemFilter
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.tileentity.vanilla.ItemStorageVanillaTileEntity
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.data.WatchedMap
import xyz.xenondevs.nova.util.emptyEnumMap
import xyz.xenondevs.nova.util.runTask
import java.util.*

private val DEFAULT_PRIORITIES = { CUBE_FACES.associateWithTo(emptyEnumMap()) { 50 } }
private val DEFAULT_CHANNELS = { CUBE_FACES.associateWithTo(emptyEnumMap()) { 0 } }

abstract class VanillaItemHolder(
    final override val endPoint: ItemStorageVanillaTileEntity
) : ItemHolder {
    
    override val itemConfig: MutableMap<BlockFace, NetworkConnectionType> =
        endPoint.retrieveDoubleEnumMap("itemConfig") { CUBE_FACES.associateWithTo(EnumMap(BlockFace::class.java)) { NetworkConnectionType.BUFFER } }
    
    open override val allowedConnectionTypes: Map<NetworkedInventory, NetworkConnectionType> by lazy {
        inventories.entries.associate { (_, inv) -> inv to NetworkConnectionType.BUFFER }
    }
    
    override val insertFilters: MutableMap<BlockFace, ItemFilter> =
        WatchedMap(
            endPoint.retrieveEnumMap<BlockFace, CompoundElement>("insertFilters", ::emptyEnumMap)
                .mapValuesTo(emptyEnumMap()) { ItemFilter(it.value) },
            ::saveData
        )
    
    override val extractFilters: MutableMap<BlockFace, ItemFilter> =
        WatchedMap(
            endPoint.retrieveEnumMap<BlockFace, CompoundElement>("extractFilters", ::emptyEnumMap)
                .mapValuesTo(emptyEnumMap()) { ItemFilter(it.value) },
            ::saveData
        )
    
    override val insertPriorities: MutableMap<BlockFace, Int> =
        WatchedMap(endPoint.retrieveEnumMap("insertPriorities", DEFAULT_PRIORITIES), ::saveData)
    
    override val extractPriorities: MutableMap<BlockFace, Int> =
        WatchedMap(endPoint.retrieveEnumMap("extractPriorities", DEFAULT_PRIORITIES), ::saveData)
    
    override val channels: MutableMap<BlockFace, Int> =
        WatchedMap(endPoint.retrieveEnumMap("channels", DEFAULT_CHANNELS), ::saveData)
    
    override fun saveData() {
        runTask {
            endPoint.storeEnumMap("itemConfig", itemConfig)
            endPoint.storeEnumMap("insertFilters", insertFilters) { it.compound }
            endPoint.storeEnumMap("extractFilters", extractFilters) { it.compound }
            endPoint.storeEnumMap("insertPriorities", insertPriorities)
            endPoint.storeEnumMap("extractPriorities", extractPriorities)
            endPoint.storeEnumMap("channels", channels)
            
            endPoint.updateDataContainer()
        }
    }
    
}

class StaticVanillaItemHolder(
    endPoint: ItemStorageVanillaTileEntity,
    override val inventories: MutableMap<BlockFace, NetworkedInventory>
) : VanillaItemHolder(endPoint)

class DynamicVanillaItemHolder(
    endPoint: ItemStorageVanillaTileEntity,
    val inventoriesGetter: () -> MutableMap<BlockFace, NetworkedInventory>,
    val allowedConnectionTypesGetter: () -> Map<NetworkedInventory, NetworkConnectionType>
) : VanillaItemHolder(endPoint) {
    
    override val inventories: MutableMap<BlockFace, NetworkedInventory>
        get() = inventoriesGetter()
    
    override val allowedConnectionTypes: Map<NetworkedInventory, NetworkConnectionType>
        get() = allowedConnectionTypesGetter()
    
}
