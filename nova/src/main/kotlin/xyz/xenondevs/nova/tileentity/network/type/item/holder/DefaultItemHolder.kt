@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.tileentity.network.type.item.holder

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.provider.entry
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.commons.collections.toEnumMap
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.mutable.defaultsToLazily
import xyz.xenondevs.commons.provider.mutable.mapNonNull
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
    compound: Provider<Compound>,
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
    
    override val containerConfig: MutableMap<BlockFace, NetworkedInventory>
        by compound.entry<Map<BlockFace, UUID>>("inventoryConfig")
            .mapNonNull(
                { it.mapValuesTo(enumMap()) { (_, uuid) -> uuidToInv[uuid]!! } },
                { it.mapValuesTo(enumMap()) { (_, inv) -> inv.uuid } }
            ).defaultsToLazily { defaultInventoryConfig().toEnumMap() }
    
    override val connectionConfig: MutableMap<BlockFace, NetworkConnectionType>
        by compound.entry<MutableMap<BlockFace, NetworkConnectionType>>("connectionConfig")
            .defaultsToLazily {
                defaultConnectionConfig?.invoke()?.toEnumMap()
                    ?: containerConfig.mapValuesTo(enumMap()) { (_, inv) -> containers[inv] }
            }
    
    override val channels: MutableMap<BlockFace, Int>
        by compound.entry<MutableMap<BlockFace, Int>>("channels")
            .defaultsToLazily(DEFAULT_CHANNELS)
    
    override val insertFilters: MutableMap<BlockFace, ItemFilter>
        by compound.entry<MutableMap<BlockFace, ItemFilter>>("insertFilters")
            .defaultsToLazily(::enumMap)
    
    override val extractFilters: MutableMap<BlockFace, ItemFilter>
        by compound.entry<MutableMap<BlockFace, ItemFilter>>("extractFilters")
            .defaultsToLazily(::enumMap)
    
    override val insertPriorities: MutableMap<BlockFace, Int>
        by compound.entry<MutableMap<BlockFace, Int>>("insertPriorities")
            .defaultsToLazily(DEFAULT_PRIORITIES)
    
    override val extractPriorities: MutableMap<BlockFace, Int>
        by compound.entry<MutableMap<BlockFace, Int>>("extractPriorities")
            .defaultsToLazily(DEFAULT_PRIORITIES)
    
    fun getNetworkedInventory(inv: VirtualInventory): NetworkedInventory =
        getNetworkedInventory(inv.uuid)
    
    fun getNetworkedInventory(uuid: UUID): NetworkedInventory =
        uuidToInv[uuid] ?: throw NoSuchElementException("No networked inventory with $uuid")
    
    private fun getUUID(inv: NetworkedInventory): UUID =
        invToUuid[inv] ?: throw NoSuchElementException("NetworkedInventory is not part of item holder")
    
    internal companion object {
        val ALL_INVENTORY_UUID = UUID(0, 0xA11)
        val DEFAULT_PRIORITIES = { CUBE_FACES.associateWithTo(enumMap()) { 50 } }
        val DEFAULT_CHANNELS = { CUBE_FACES.associateWithTo(enumMap()) { 0 } }
    }
    
}