@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.tileentity.network.type.item.holder

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.commons.collections.toEnumMap
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.nova.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.type.item.ItemFilter
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.util.CUBE_FACES
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
    override val compound: Compound,
    override val containers: Map<NetworkedInventory, NetworkConnectionType>,
    override val mergedInventory: NetworkedInventory?,
    defaultInventoryConfig: () -> Map<BlockFace, NetworkedInventory>,
    defaultConnectionConfig: (() -> Map<BlockFace, NetworkConnectionType>)?
) : ItemHolder {
    
    init {
        if (containers.isEmpty())
            throw IllegalArgumentException("availableInventories must not be empty")
    }
    
    private val invToUuid: BiMap<NetworkedInventory, UUID> = containers.keys.associateWithTo(HashBiMap.create()) { it.uuid }
    private val uuidToInv: BiMap<UUID, NetworkedInventory> get() = invToUuid.inverse()
    
    override val containerConfig: MutableMap<BlockFace, NetworkedInventory> =
        compound.get<Map<BlockFace, UUID>>("inventoryConfig")
            ?.mapValuesTo(enumMap()) { uuidToInv[it.value]!! }
            ?: defaultInventoryConfig().toEnumMap()
    
    override val connectionConfig: MutableMap<BlockFace, NetworkConnectionType> =
        compound["connectionConfig"]
            ?: defaultConnectionConfig?.invoke()?.toEnumMap() 
            ?: containerConfig.mapValuesTo(enumMap()) { (_, inv) -> containers[inv] }
    
    override val channels: MutableMap<BlockFace, Int> =
        compound["channels"] ?: DEFAULT_CHANNELS()
    
    override val insertFilters: MutableMap<BlockFace, ItemFilter> =
        compound["insertFilters"] ?: enumMap()
    
    override val extractFilters: MutableMap<BlockFace, ItemFilter> =
        compound["extractFilters"] ?: enumMap()
    
    override val insertPriorities: MutableMap<BlockFace, Int> =
        compound["insertPriorities"] ?: DEFAULT_PRIORITIES()
    
    override val extractPriorities: MutableMap<BlockFace, Int> =
        compound["extractPriorities"] ?: DEFAULT_PRIORITIES()
    
    fun getNetworkedInventory(inv: VirtualInventory): NetworkedInventory =
        getNetworkedInventory(inv.uuid)
    
    fun getNetworkedInventory(uuid: UUID): NetworkedInventory =
        uuidToInv[uuid] ?: throw NoSuchElementException("No networked inventory with $uuid")
    
    private fun getUUID(inv: NetworkedInventory): UUID =
        invToUuid[inv] ?: throw NoSuchElementException("NetworkedInventory is not part of item holder")
    
    override fun saveData() {
        compound["inventoryConfig"] = containerConfig.mapValuesTo(enumMap()) { (_, inv) -> getUUID(inv) }
        compound["connectionConfig"] = connectionConfig
        compound["channels"] = channels
        compound["insertFilters"] = insertFilters
        compound["extractFilters"] = extractFilters
        compound["insertPriorities"] = insertPriorities
        compound["extractPriorities"] = extractPriorities
    }
    
    internal companion object {
        val ALL_INVENTORY_UUID = UUID(0, 0xA11)
        val DEFAULT_PRIORITIES = { CUBE_FACES.associateWithTo(enumMap()) { 50 } }
        val DEFAULT_CHANNELS = { CUBE_FACES.associateWithTo(enumMap()) { 0 } }
    }
    
}