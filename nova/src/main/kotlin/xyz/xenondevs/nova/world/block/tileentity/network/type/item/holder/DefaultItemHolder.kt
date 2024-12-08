@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.entry
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.observed
import xyz.xenondevs.commons.provider.orElseNew
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.nova.serialization.DataHolder
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.world.block.tileentity.network.node.DefaultContainerEndPointDataHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.ItemFilter
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.NetworkedInventory
import java.util.*

/**
 * The default [ItemHolder] implementation.
 *
 * @param compound the [Compound] for data storage and retrieval
 * @param containers all available [NetworkedInventories][NetworkedInventory] and their allowed [NetworkConnectionType]
 * @param mergedInventory the [NetworkedInventory] that is the merged inventory of all [NetworkedInventories][NetworkedInventory],
 * or null if there is no merged inventory
 * @param defaultInventoryConfig the default ([BlockFace], [NetworkedInventory]) to be used if no configuration is stored
 * @param defaultConnectionConfig the default ([BlockFace], [NetworkConnectionType]) to be used if no configuration is stored.
 * If null, the connection config will be automatically generated using the highest possible connection type for each side.
 */
class DefaultItemHolder(
    compound: Provider<Compound>,
    containers: Map<NetworkedInventory, NetworkConnectionType>,
    override val mergedInventory: NetworkedInventory?,
    blockedFaces: Set<BlockFace>,
    defaultInventoryConfig: () -> Map<BlockFace, NetworkedInventory>,
    defaultConnectionConfig: (() -> Map<BlockFace, NetworkConnectionType>)?
) : DefaultContainerEndPointDataHolder<NetworkedInventory>(
    compound,
    containers,
    blockedFaces,
    defaultInventoryConfig,
    defaultConnectionConfig
), ItemHolder {
    
    init {
        if (containers.isEmpty())
            throw IllegalArgumentException("availableInventories must not be empty")
    }
    
    override val uuidToContainer: Map<UUID, NetworkedInventory> = containers.keys.associateByTo(HashMap()) { it.uuid }
    
    override val insertFilters: MutableMap<BlockFace, ItemFilter<*>>
        by compound.entry<MutableMap<BlockFace, ItemFilter<*>>>("insertFilters")
            .orElseNew(::enumMap)
            .observed()
    
    override val extractFilters: MutableMap<BlockFace, ItemFilter<*>>
        by compound.entry<MutableMap<BlockFace, ItemFilter<*>>>("extractFilters")
            .orElseNew(::enumMap)
            .observed()
    
    fun getNetworkedInventory(inv: VirtualInventory): NetworkedInventory =
        getNetworkedInventory(inv.uuid)
    
    fun getNetworkedInventory(uuid: UUID): NetworkedInventory =
        uuidToContainer[uuid] ?: throw NoSuchElementException("No networked inventory with $uuid")
    
    internal companion object {
        
        val ALL_INVENTORY_UUID = UUID(0, 0xA11)
        val DEFAULT_PRIORITIES = { CUBE_FACES.associateWithTo(enumMap()) { 50 } }
        val DEFAULT_CHANNELS = { CUBE_FACES.associateWithTo(enumMap()) { 0 } }
        
        fun tryConvertLegacy(dataHolder: DataHolder): Compound? {
            val inventoryConfig: MutableMap<BlockFace, UUID>? =
                dataHolder.retrieveDataOrNull("inventories")
            val connectionConfig: MutableMap<BlockFace, NetworkConnectionType>? =
                dataHolder.retrieveDataOrNull("itemConfig")
            val insertFilters: MutableMap<BlockFace, ItemFilter<*>>? =
                dataHolder.retrieveDataOrNull("insertFilters")
            val extractFilters: MutableMap<BlockFace, ItemFilter<*>>? =
                dataHolder.retrieveDataOrNull("extractFilters")
            val insertPriorities: MutableMap<BlockFace, Int>? =
                dataHolder.retrieveDataOrNull("insertPriorities")
            val extractPriorities: MutableMap<BlockFace, Int>? =
                dataHolder.retrieveDataOrNull("extractPriorities")
            val channels: MutableMap<BlockFace, Int>? =
                dataHolder.retrieveDataOrNull("channels")
            
            if (inventoryConfig == null &&
                connectionConfig == null &&
                insertFilters == null &&
                extractFilters == null &&
                insertPriorities == null &&
                extractPriorities == null &&
                channels == null
            ) return null
            
            dataHolder.removeData("inventories")
            dataHolder.removeData("itemConfig")
            dataHolder.removeData("insertFilters")
            dataHolder.removeData("extractFilters")
            dataHolder.removeData("insertPriorities")
            dataHolder.removeData("extractPriorities")
            dataHolder.removeData("channels")
            
            val compound = Compound() // new format
            if (inventoryConfig != null)
                compound["inventoryConfig"] = inventoryConfig
            if (connectionConfig != null)
                compound["connectionConfig"] = connectionConfig
            if (insertFilters != null)
                compound["insertFilters"] = insertFilters
            if (extractFilters != null)
                compound["extractFilters"] = extractFilters
            if (insertPriorities != null)
                compound["insertPriorities"] = insertPriorities
            if (extractPriorities != null)
                compound["extractPriorities"] = extractPriorities
            if (channels != null)
                compound["channels"] = channels
            
            return compound
        }
        
    }
    
}