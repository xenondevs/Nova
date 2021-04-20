package xyz.xenondevs.nova.tileentity.vanilla

import com.google.gson.JsonObject
import org.bukkit.block.BlockFace
import org.bukkit.block.Container
import org.bukkit.block.Furnace
import org.bukkit.block.TileState
import xyz.xenondevs.nova.network.Network
import xyz.xenondevs.nova.network.NetworkManager
import xyz.xenondevs.nova.network.NetworkType
import xyz.xenondevs.nova.network.item.ItemConnectionType
import xyz.xenondevs.nova.network.item.ItemStorage
import xyz.xenondevs.nova.network.item.inventory.NetworkedBukkitInventory
import xyz.xenondevs.nova.network.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.network.item.inventory.NetworkedRangedBukkitInventory
import xyz.xenondevs.nova.tileentity.TILE_ENTITY_KEY
import xyz.xenondevs.nova.tileentity.serialization.JsonElementDataType
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.GSON
import xyz.xenondevs.nova.util.enumMapOf
import xyz.xenondevs.nova.util.fromJson
import java.util.*


private fun TileState.hasTileEntityData() =
    persistentDataContainer.has(TILE_ENTITY_KEY, JsonElementDataType)

private fun TileState.getTileEntityData() =
    persistentDataContainer.get(TILE_ENTITY_KEY, JsonElementDataType) as JsonObject

abstract class VanillaTileEntity(private val tileState: TileState) {
    
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
        tileState.persistentDataContainer.set(TILE_ENTITY_KEY, JsonElementDataType, dataObject)
        tileState.update()
    }
    
}

abstract class ItemStorageVanillaTileEntity(tileState: TileState) : VanillaTileEntity(tileState), ItemStorage {
    
    override val location = tileState.location
    
    override val networks: MutableMap<NetworkType, MutableMap<BlockFace, Network>> =
        EnumMap(NetworkType::class.java)
    
    final override val itemConfig: MutableMap<BlockFace, ItemConnectionType> =
        retrieveData(
            CUBE_FACES.associateWithTo(EnumMap(BlockFace::class.java)) { ItemConnectionType.INSERT },
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
        storeData("itemConfig", itemConfig)
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
