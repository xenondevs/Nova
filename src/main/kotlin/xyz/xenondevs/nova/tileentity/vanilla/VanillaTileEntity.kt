package xyz.xenondevs.nova.tileentity.vanilla

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
import xyz.xenondevs.nova.serialization.DataHolder
import xyz.xenondevs.nova.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.serialization.persistentdata.CompoundElementDataType
import xyz.xenondevs.nova.tileentity.TILE_ENTITY_KEY
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.emptyEnumMap
import xyz.xenondevs.nova.util.enumMapOf
import java.util.*
import kotlin.collections.set

private fun TileState.hasTileEntityData() =
    persistentDataContainer.has(TILE_ENTITY_KEY, CompoundElementDataType)

private fun TileState.getTileEntityData(): CompoundElement {
    if (hasTileEntityData())
        return persistentDataContainer.get(TILE_ENTITY_KEY, CompoundElementDataType) as CompoundElement
    return CompoundElement()
}

private val EMPTY_INVENTORY: NetworkedInventory = object : NetworkedInventory {
    override val size = 0
    override val items = emptyArray<ItemStack?>()
    override fun addItem(item: ItemStack) = item
    override fun setItem(slot: Int, item: ItemStack?) = Unit
}

private val EMPTY_INVENTORIES_MAP = CUBE_FACES.associateWithTo(emptyEnumMap()) { EMPTY_INVENTORY }

abstract class VanillaTileEntity(tileState: TileState) : DataHolder(tileState.getTileEntityData(), false) {
    
    val block = tileState.block
    val type = block.type
    
    abstract fun handleRemoved(unload: Boolean)
    
    abstract fun handleInitialized()
    
    fun updateDataContainer() {
        val tileState = block.state as TileState
        tileState.persistentDataContainer.set(TILE_ENTITY_KEY, CompoundElementDataType, data)
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
        retrieveDoubleEnumMap("itemConfig") { CUBE_FACES.associateWithTo(EnumMap(BlockFace::class.java)) { ItemConnectionType.BUFFER } }
    
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
        if (unload) {
            storeEnumMap("itemConfig", itemConfig)
            updateDataContainer()
        }
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
