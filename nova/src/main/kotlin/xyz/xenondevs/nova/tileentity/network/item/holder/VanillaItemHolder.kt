package xyz.xenondevs.nova.tileentity.network.item.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.item.ItemFilter
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.tileentity.vanilla.ItemStorageVanillaTileEntity
import xyz.xenondevs.nova.util.CUBE_FACES
import java.util.*

private val DEFAULT_PRIORITIES = { CUBE_FACES.associateWithTo(enumMap()) { 50 } }
private val DEFAULT_CHANNELS = { CUBE_FACES.associateWithTo(enumMap()) { 0 } }

internal abstract class VanillaItemHolder(
    final override val endPoint: ItemStorageVanillaTileEntity
) : ItemHolder {
    
    override val mergedInventory: NetworkedInventory? = null
    
    override val connectionConfig: MutableMap<BlockFace, NetworkConnectionType> =
        endPoint.retrieveData("itemConfig") { CUBE_FACES.associateWithTo(enumMap()) { NetworkConnectionType.INSERT } }
    
    override val allowedConnectionTypes: Map<NetworkedInventory, NetworkConnectionType> by lazy {
        containerConfig.entries.associate { (_, inv) -> inv to NetworkConnectionType.BUFFER }
    }
    
    override val insertFilters: MutableMap<BlockFace, ItemFilter> =
        endPoint.retrieveData<EnumMap<BlockFace, ItemFilter>>("insertFilters", ::enumMap)
    
    override val extractFilters: MutableMap<BlockFace, ItemFilter> =
        endPoint.retrieveData<EnumMap<BlockFace, ItemFilter>>("extractFilters", ::enumMap)
    
    override val insertPriorities: MutableMap<BlockFace, Int> =
        endPoint.retrieveData("insertPriorities", DEFAULT_PRIORITIES)
    
    override val extractPriorities: MutableMap<BlockFace, Int> =
        endPoint.retrieveData("extractPriorities", DEFAULT_PRIORITIES)
    
    override val channels: MutableMap<BlockFace, Int> =
        endPoint.retrieveData("channels", DEFAULT_CHANNELS)
    
    override fun saveData() {
        endPoint.storeData("itemConfig", connectionConfig)
        endPoint.storeData("insertFilters", insertFilters)
        endPoint.storeData("extractFilters", extractFilters)
        endPoint.storeData("insertPriorities", insertPriorities)
        endPoint.storeData("extractPriorities", extractPriorities)
        endPoint.storeData("channels", channels)
    }
    
}

internal class StaticVanillaItemHolder(
    endPoint: ItemStorageVanillaTileEntity,
    override val containerConfig: MutableMap<BlockFace, NetworkedInventory>
) : VanillaItemHolder(endPoint)

internal class DynamicVanillaItemHolder(
    endPoint: ItemStorageVanillaTileEntity,
    val inventoriesGetter: () -> MutableMap<BlockFace, NetworkedInventory>,
    val allowedConnectionTypesGetter: () -> Map<NetworkedInventory, NetworkConnectionType>
) : VanillaItemHolder(endPoint) {
    
    override val containerConfig: MutableMap<BlockFace, NetworkedInventory>
        get() = inventoriesGetter()
    
    override val allowedConnectionTypes: Map<NetworkedInventory, NetworkConnectionType>
        get() = allowedConnectionTypesGetter()
    
}
