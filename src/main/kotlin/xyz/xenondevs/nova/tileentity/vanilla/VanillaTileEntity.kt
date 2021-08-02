package xyz.xenondevs.nova.tileentity.vanilla

import com.google.gson.JsonObject
import org.bukkit.block.*
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.network.Network
import xyz.xenondevs.nova.network.NetworkManager
import xyz.xenondevs.nova.network.NetworkNode
import xyz.xenondevs.nova.network.NetworkType
import xyz.xenondevs.nova.network.item.ItemConnectionType
import xyz.xenondevs.nova.network.item.ItemStorage
import xyz.xenondevs.nova.network.item.inventory.NetworkedBukkitInventory
import xyz.xenondevs.nova.network.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.network.item.inventory.NetworkedRangedBukkitInventory
import xyz.xenondevs.nova.serialization.persistentdata.JsonElementDataType
import xyz.xenondevs.nova.tileentity.TILE_ENTITY_KEY
import xyz.xenondevs.nova.util.*
import java.util.*

private fun TileState.hasTileEntityData() =
    persistentDataContainer.has(TILE_ENTITY_KEY, JsonElementDataType)

private fun TileState.getTileEntityData() =
    persistentDataContainer.get(TILE_ENTITY_KEY, JsonElementDataType) as JsonObject

private val EMPTY_INVENTORY: NetworkedInventory = object : NetworkedInventory {
    override val size = 0
    override val items = emptyArray<ItemStack?>()
    override fun addItem(item: ItemStack) = item
    override fun setItem(slot: Int, item: ItemStack?) = Unit
}

private val EMPTY_INVENTORIES_MAP = CUBE_FACES.associateWithTo(emptyEnumMap()) { EMPTY_INVENTORY }

abstract class VanillaTileEntity(tileState: TileState) {
    
    val block = tileState.block
    val type = block.type
    protected val dataObject: JsonObject = if (tileState.hasTileEntityData()) tileState.getTileEntityData() else JsonObject()
    
    abstract fun handleRemoved(unload: Boolean)
    
    abstract fun handleInitialized()
    
    protected inline fun <reified T> retrieveData(alternative: T, key: String): T {
        return retrieveOrNull(key) ?: alternative
    }
    
    protected inline fun <reified T> retrieveOrNull(key: String): T? {
        return GSON.fromJson<T>(dataObject.get(key))
    }
    
    fun storeData(key: String, value: Any) {
        dataObject.add(key, GSON.toJsonTree(value))
        val tileState = block.state as TileState
        tileState.persistentDataContainer.set(TILE_ENTITY_KEY, JsonElementDataType, dataObject)
        tileState.update()
    }
    
}

abstract class ItemStorageVanillaTileEntity(tileState: TileState) : VanillaTileEntity(tileState), ItemStorage {
    
    override val location = tileState.location
    
    final override val networks: MutableMap<NetworkType, MutableMap<BlockFace, Network>> =
        NetworkType.values().associateWithTo(emptyEnumMap()) { enumMapOf() }
    final override val connectedNodes: MutableMap<NetworkType, MutableMap<BlockFace, NetworkNode>> =
        NetworkType.values().associateWithTo(emptyEnumMap()) { enumMapOf() }
    
    final override val itemConfig: MutableMap<BlockFace, ItemConnectionType> =
        retrieveData(
            CUBE_FACES.associateWithTo(EnumMap(BlockFace::class.java)) { ItemConnectionType.BUFFER },
            "itemConfig"
        )
    
    final override val allowedFaces: Map<NetworkType, List<BlockFace>>
        get() = enumMapOf(
            NetworkType.ITEMS to itemConfig
                .filter { it.value != ItemConnectionType.NONE }
                .map { it.key }
        )
    
    override fun handleInitialized() {
        NetworkManager.handleEndPointAdd(this)
    }
    
    override fun handleRemoved(unload: Boolean) {
        if (unload) storeData("itemConfig", itemConfig)
        NetworkManager.handleEndPointRemove(this, unload)
    }
    
}

class VanillaContainerTileEntity(container: Container) : ItemStorageVanillaTileEntity(container) {
    
    override val inventories: MutableMap<BlockFace, NetworkedInventory>
    
    init {
        val inventory = NetworkedBukkitInventory(container.inventory)
        inventories = CUBE_FACES.associateWithTo(EnumMap(BlockFace::class.java)) { inventory }
    }
    
}

class VanillaChestTileEntity(chest: Chest) : ItemStorageVanillaTileEntity(chest) {
    
    private var inventory = chest.inventory
    
    override val inventories: EnumMap<BlockFace, NetworkedInventory>
        get() {
            val state = location.block.state
            return if (state is Chest) {
                if (state.inventory.size != inventory.size) inventory = state.inventory
                val inventory = NetworkedBukkitInventory(inventory)
                CUBE_FACES.associateWithTo(EnumMap(BlockFace::class.java)) { inventory }
            } else EMPTY_INVENTORIES_MAP
        }
    
}

class VanillaFurnaceTileEntity(furnace: Furnace) : ItemStorageVanillaTileEntity(furnace) {
    
    override val inventories: MutableMap<BlockFace, NetworkedInventory>
    
    init {
        val bukkitInventory = furnace.inventory
        val inputInventory = NetworkedRangedBukkitInventory(bukkitInventory, 0)
        val fuelInventory = NetworkedRangedBukkitInventory(bukkitInventory, 1)
        val outputInventory = NetworkedRangedBukkitInventory(bukkitInventory, 2)
        
        inventories = CUBE_FACES.associateWithTo(EnumMap(BlockFace::class.java)) { fuelInventory }
        inventories[BlockFace.UP] = inputInventory
        inventories[BlockFace.DOWN] = outputInventory
    }
    
}
