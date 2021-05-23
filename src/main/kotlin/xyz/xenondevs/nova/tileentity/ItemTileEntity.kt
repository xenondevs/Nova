package xyz.xenondevs.nova.tileentity

import com.google.gson.JsonObject
import de.studiocode.invui.virtualinventory.VirtualInventory
import org.bukkit.block.BlockFace
import org.bukkit.entity.ArmorStand
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.item.ItemConnectionType
import xyz.xenondevs.nova.network.item.ItemStorage
import xyz.xenondevs.nova.network.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.network.item.inventory.NetworkedVirtualInventory
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.enumMapOf
import java.util.*

abstract class ItemTileEntity(
    ownerUUID: UUID?,
    material: NovaMaterial,
    data: JsonObject,
    armorStand: ArmorStand
) : NetworkedTileEntity(ownerUUID, material, data, armorStand), ItemStorage {
    
    final override val inventories: MutableMap<BlockFace, NetworkedInventory> by lazy {
        (retrieveOrNull<Map<BlockFace, UUID>>("inventories") ?: defaultInventoryConfig)
            .mapValuesTo(enumMapOf()) { availableInventories[it.value]!! }
    }
    
    final override val itemConfig: MutableMap<BlockFace, ItemConnectionType> =
        retrieveData("itemConfig") { CUBE_FACES.associateWithTo(EnumMap(BlockFace::class.java)) { ItemConnectionType.NONE } }
    
    lateinit var defaultInventoryConfig: Map<BlockFace, UUID>
    private val availableInventories: MutableMap<UUID, NetworkedInventory> = mutableMapOf()
    
    fun addAvailableInventories(vararg pairs: Pair<UUID, NetworkedInventory>) =
        availableInventories.putAll(pairs)
    
    fun addAvailableInventories(vararg virtualInventories: VirtualInventory) =
        virtualInventories.forEach { availableInventories[it.uuid] = NetworkedVirtualInventory(it) }
    
    fun setDefaultInventory(networkedInventory: NetworkedInventory) {
        val uuid = findUUID(networkedInventory) ?: throw IllegalStateException("Inventory is not available")
        defaultInventoryConfig = CUBE_FACES.associateWith { uuid }
    }
    
    fun setDefaultInventory(virtualInventory: VirtualInventory) {
        val uuid = virtualInventory.uuid
        if (availableInventories[uuid] == null)
            availableInventories[uuid] = NetworkedVirtualInventory(virtualInventory)
        
        defaultInventoryConfig = CUBE_FACES.associateWith { uuid }
    }
    
    fun getNetworkedInventory(virtualInventory: VirtualInventory) =
        availableInventories[virtualInventory.uuid]!!
    
    fun getNetworkedInventory(uuid: UUID) =
        availableInventories[uuid]!!
    
    private fun findUUID(networkedInventory: NetworkedInventory) =
        availableInventories.firstNotNullOfOrNull { if (it.value == networkedInventory) it.key else null }
    
    override fun saveData() {
        super.saveData()
        storeData("itemConfig", itemConfig)
        storeData("inventories", inventories.mapValues { findUUID(it.value) })
    }
    
}

