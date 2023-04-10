@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.tileentity.network.item.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.item.ItemFilter
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedMultiVirtualInventory
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedVirtualInventory
import xyz.xenondevs.nova.util.CUBE_FACES
import java.util.*

fun NovaItemHolder(
    endPoint: NetworkedTileEntity,
    inventory: Pair<VirtualInventory, NetworkConnectionType>,
    vararg moreInventories: Pair<VirtualInventory, NetworkConnectionType>,
    defaultInvConfig: (() -> EnumMap<BlockFace, UUID>) = { CUBE_FACES.associateWithTo(enumMap()) { NovaItemHolder.ALL_INVENTORY_UUID } },
    defaultConnectionConfig: (() -> EnumMap<BlockFace, NetworkConnectionType>)? = null
): NovaItemHolder {
    val virtualInventories = arrayListOf(inventory).apply { addAll(moreInventories) }
    val networkedInventories: Map<VirtualInventory, NetworkedInventory> = virtualInventories.associateTo(HashMap()) { (vi, _) -> vi to NetworkedVirtualInventory(vi) }
    val availableInventories: MutableMap<UUID, NetworkedInventory> = virtualInventories.associateTo(HashMap()) { (vi, _) -> vi.uuid to networkedInventories[vi]!! }
    val allowedConnectionTypes: MutableMap<NetworkedInventory, NetworkConnectionType> = virtualInventories.associateTo(HashMap()) { (vi, pc) -> networkedInventories[vi]!! to pc }
    
    val mergedInventory = NetworkedMultiVirtualInventory(virtualInventories)
    availableInventories[NovaItemHolder.ALL_INVENTORY_UUID] = mergedInventory
    allowedConnectionTypes[mergedInventory] = NetworkConnectionType.of(allowedConnectionTypes.values)
    
    return NovaItemHolder(
        endPoint,
        availableInventories,
        allowedConnectionTypes,
        mergedInventory,
        defaultInvConfig,
        defaultConnectionConfig
    )
}

@JvmName("NovaItemHolder1")
fun NovaItemHolder(
    endPoint: NetworkedTileEntity,
    defaultInventory: Pair<UUID, Pair<NetworkedInventory, NetworkConnectionType>>,
    vararg otherInventories: Pair<UUID, Pair<NetworkedInventory, NetworkConnectionType>>,
    defaultInvConfig: (() -> EnumMap<BlockFace, UUID>) = { CUBE_FACES.associateWithTo(enumMap()) { defaultInventory.first } },
    defaultConnectionConfig: (() -> EnumMap<BlockFace, NetworkConnectionType>)? = null
): NovaItemHolder {
    val allInventories = hashMapOf(defaultInventory).apply { putAll(otherInventories) }
    val networkedInventories = allInventories.mapValuesTo(HashMap()) { (_, pair) -> pair.first }
    val allowedConnectionTypes = allInventories.entries.associateTo(HashMap()) { (_, pair) -> pair }
    
    return NovaItemHolder(
        endPoint,
        networkedInventories,
        allowedConnectionTypes,
        null,
        defaultInvConfig,
        defaultConnectionConfig
    )
}

class NovaItemHolder internal constructor(
    override val endPoint: NetworkedTileEntity,
    val availableInventories: Map<UUID, NetworkedInventory>,
    override val allowedConnectionTypes: Map<NetworkedInventory, NetworkConnectionType>,
    override val mergedInventory: NetworkedInventory?,
    defaultInvConfig: () -> EnumMap<BlockFace, UUID>,
    defaultConnectionConfig: (() -> EnumMap<BlockFace, NetworkConnectionType>)?
) : ItemHolder {
    
    override val containerConfig: MutableMap<BlockFace, NetworkedInventory> =
        (endPoint.retrieveDataOrNull<EnumMap<BlockFace, UUID>>("inventories") ?: defaultInvConfig())
            .mapValuesTo(enumMap()) { availableInventories[it.value]!! }
    
    override val connectionConfig: MutableMap<BlockFace, NetworkConnectionType> =
        endPoint.retrieveData("itemConfig", defaultConnectionConfig
            ?: { CUBE_FACES.associateWithTo(enumMap()) { NetworkConnectionType.NONE } })
    
    override val insertFilters: MutableMap<BlockFace, ItemFilter> =
        endPoint.retrieveData<EnumMap<BlockFace, ItemFilter>>("insertFilters", ::enumMap)
    
    override val extractFilters: MutableMap<BlockFace, ItemFilter> =
        endPoint.retrieveData<EnumMap<BlockFace, ItemFilter>>("extractFilters", ::enumMap)
    
    override val insertPriorities: MutableMap<BlockFace, Int> =
        endPoint.retrieveData("insertPriorities") { CUBE_FACES.associateWithTo(enumMap()) { 50 } }
    
    override val extractPriorities: MutableMap<BlockFace, Int> =
        endPoint.retrieveData("extractPriorities") { CUBE_FACES.associateWithTo(enumMap()) { 50 } }
    
    override val channels: MutableMap<BlockFace, Int> =
        endPoint.retrieveData("channels") { CUBE_FACES.associateWithTo(enumMap()) { 0 } }
    
    fun getNetworkedInventory(virtualInventory: VirtualInventory): NetworkedInventory =
        availableInventories[virtualInventory.uuid]!!
    
    fun getNetworkedInventory(uuid: UUID): NetworkedInventory =
        availableInventories[uuid]!!
    
    private fun findUUID(networkedInventory: NetworkedInventory): UUID? =
        availableInventories.firstNotNullOfOrNull { if (it.value == networkedInventory) it.key else null }
    
    override fun saveData() {
        endPoint.storeData("itemConfig", connectionConfig)
        endPoint.storeData("insertFilters", insertFilters)
        endPoint.storeData("extractFilters", extractFilters)
        endPoint.storeData("channels", channels)
        endPoint.storeData("insertPriorities", insertPriorities)
        endPoint.storeData("extractPriorities", extractPriorities)
        
        if (availableInventories.isNotEmpty())
            endPoint.storeData("inventories", containerConfig.mapValuesTo(enumMap()) { findUUID(it.value)!! })
    }
    
    companion object {
        val ALL_INVENTORY_UUID = UUID(0, 0xA11)
    }
    
}