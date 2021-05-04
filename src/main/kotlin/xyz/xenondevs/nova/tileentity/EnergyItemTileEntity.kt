package xyz.xenondevs.nova.tileentity

import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.virtualinventory.VirtualInventoryManager
import org.bukkit.block.BlockFace
import org.bukkit.entity.ArmorStand
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.item.ItemConnectionType
import xyz.xenondevs.nova.network.item.ItemStorage
import xyz.xenondevs.nova.network.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.network.item.inventory.NetworkedVirtualInventory
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.filterIsInstanceValues
import java.util.*

abstract class EnergyItemTileEntity(
    ownerUUID: UUID?,
    material: NovaMaterial,
    armorStand: ArmorStand
) : EnergyTileEntity(ownerUUID, material, armorStand), ItemStorage {
    
    final override lateinit var inventories: MutableMap<BlockFace, NetworkedInventory>
    
    override val itemConfig: MutableMap<BlockFace, ItemConnectionType> =
        retrieveData("itemConfig") { CUBE_FACES.associateWithTo(EnumMap(BlockFace::class.java)) { ItemConnectionType.NONE } }
    
    init {
        val inventoryConfig = retrieveData<Map<BlockFace, UUID>>("inventoryConfig") { emptyMap() }
            .mapValuesTo(EnumMap(BlockFace::class.java)) { VirtualInventoryManager.getInstance().getByUuid(it.value) }
        
        if (inventoryConfig.isNotEmpty()) initInventories(inventoryConfig)
    }
    
    private fun initInventories(inventoryConfig: Map<BlockFace, VirtualInventory>) {
        inventories = inventoryConfig.mapValuesTo(EnumMap(BlockFace::class.java)) { NetworkedVirtualInventory(it.value) }
    }
    
    @JvmName("initInventories1")
    private fun initInventories(inventoryConfig: Map<BlockFace, NetworkedInventory>) {
        inventories = inventoryConfig.mapValuesTo(EnumMap(BlockFace::class.java)) { it.value }
    }
    
    fun setDefaultInventoryConfig(inventoryConfig: Map<BlockFace, VirtualInventory>) {
        if (!::inventories.isInitialized) initInventories(inventoryConfig)
    }
    
    @JvmName("setDefaultInventoryConfig1")
    fun setDefaultNetworkedInventoryConfig(inventoryConfig: Map<BlockFace, NetworkedInventory>) {
        if (!::inventories.isInitialized) initInventories(inventoryConfig)
    }
    
    fun setDefaultInventory(inventory: VirtualInventory) {
        setDefaultInventoryConfig(CUBE_FACES.associateWith { inventory })
    }
    
    fun setDefaultInventory(inventory: NetworkedInventory) {
        setDefaultNetworkedInventoryConfig(CUBE_FACES.associateWith { inventory })
    }
    
    fun getNetworkedInventory(inventory: VirtualInventory): NetworkedInventory {
        return inventories.values
            .filterIsInstance<NetworkedVirtualInventory>()
            .firstOrNull { it.virtualInventory == inventory }
            ?: NetworkedVirtualInventory(inventory)
    }
    
    override fun saveData() {
        super.saveData()
        storeData("itemConfig", itemConfig)
        storeData("inventoryConfig", inventories.filterIsInstanceValues<NetworkedVirtualInventory, BlockFace, NetworkedInventory>()
            .mapValuesTo(EnumMap(BlockFace::class.java)) { it.value.virtualInventory.uuid })
    }
    
}