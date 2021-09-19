package xyz.xenondevs.nova.tileentity.network.item.holder

import de.studiocode.invui.virtualinventory.VirtualInventory
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.item.ItemConnectionType
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedVirtualInventory
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.associateWithToEnumMap
import xyz.xenondevs.nova.util.enumMapOf
import java.util.*

class NovaItemHolder(
    override val endPoint: NetworkedTileEntity,
    private val availableInventories: MutableMap<UUID, out NetworkedInventory>,
    lazyDefaultConfig: () -> MutableMap<BlockFace, UUID>
) : ItemHolder {
    
    constructor(endPoint: NetworkedTileEntity, defaultInventory: VirtualInventory, vararg otherInventories: VirtualInventory) :
        this(
            endPoint,
            hashMapOf<UUID, NetworkedInventory>(
                defaultInventory.uuid to NetworkedVirtualInventory(defaultInventory)
            ).also { otherInventories.forEach { inventory -> it[inventory.uuid] = NetworkedVirtualInventory(inventory) } },
            { CUBE_FACES.associateWithToEnumMap { defaultInventory.uuid } }
        )
    
    constructor(endPoint: NetworkedTileEntity, defaultInventory: Pair<UUID, NetworkedInventory>, vararg otherInventories: Pair<UUID, NetworkedInventory>) :
        this(
            endPoint,
            hashMapOf<UUID, NetworkedInventory>(
                defaultInventory.first to defaultInventory.second
            ).also { otherInventories.forEach { pair -> it[pair.first] = pair.second } },
            { CUBE_FACES.associateWithToEnumMap { defaultInventory.first } }
        )
    
    override val inventories: MutableMap<BlockFace, NetworkedInventory> =
        (endPoint.retrieveEnumMapOrNull("inventories") ?: lazyDefaultConfig())
            .mapValuesTo(enumMapOf()) { availableInventories[it.value]!! }
    
    override val itemConfig: MutableMap<BlockFace, ItemConnectionType> =
        endPoint.retrieveDoubleEnumMap("itemConfig") {
            CUBE_FACES.associateWithTo(EnumMap(BlockFace::class.java)) { ItemConnectionType.NONE }
        }
    
    fun getNetworkedInventory(virtualInventory: VirtualInventory) =
        availableInventories[virtualInventory.uuid]!!
    
    fun getNetworkedInventory(uuid: UUID) =
        availableInventories[uuid]!!
    
    private fun findUUID(networkedInventory: NetworkedInventory) =
        availableInventories.firstNotNullOfOrNull { if (it.value == networkedInventory) it.key else null }
    
    override fun saveData() {
        endPoint.storeEnumMap("itemConfig", itemConfig)
        
        if (availableInventories.isNotEmpty())
            endPoint.storeEnumMap("inventories", inventories.mapValues { findUUID(it.value) })
    }
    
}