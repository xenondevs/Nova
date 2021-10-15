package xyz.xenondevs.nova.tileentity.vanilla

import org.bukkit.block.*
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.serialization.DataHolder
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.data.serialization.persistentdata.CompoundElementDataType
import xyz.xenondevs.nova.tileentity.TILE_ENTITY_KEY
import xyz.xenondevs.nova.tileentity.network.*
import xyz.xenondevs.nova.tileentity.network.item.holder.DynamicVanillaItemHolder
import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.tileentity.network.item.holder.StaticVanillaItemHolder
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedBukkitInventory
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedRangedBukkitInventory
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

abstract class ItemStorageVanillaTileEntity(tileState: TileState) : VanillaTileEntity(tileState), NetworkEndPoint {
    
    abstract val itemHolder: ItemHolder
    override val location = tileState.location
    
    final override val networks: MutableMap<NetworkType, MutableMap<BlockFace, Network>> =
        NetworkType.values().associateWithTo(emptyEnumMap()) { emptyEnumMap() }
    final override val connectedNodes: MutableMap<NetworkType, MutableMap<BlockFace, NetworkNode>> =
        NetworkType.values().associateWithTo(emptyEnumMap()) { emptyEnumMap() }
    final override val holders: MutableMap<NetworkType, EndPointDataHolder>
        by lazy { enumMapOf(NetworkType.ITEMS to itemHolder) }
    
    override fun handleInitialized() {
        NetworkManager.runAsync { it.handleEndPointAdd(this) }
    }
    
    override fun handleRemoved(unload: Boolean) {
        if (unload) {
            itemHolder.saveData()
            updateDataContainer()
        }
        
        val task: NetworkManagerTask = { it.handleEndPointRemove(this, unload) }
        if (NOVA.isEnabled) NetworkManager.runAsync(task) else NetworkManager.runNow(task)
    }
    
}

class VanillaContainerTileEntity(container: Container) : ItemStorageVanillaTileEntity(container) {
    
    override val itemHolder: StaticVanillaItemHolder
    
    init {
        val inventory = NetworkedBukkitInventory(container.inventory)
        val inventories = CUBE_FACES.associateWithTo(emptyEnumMap<BlockFace, NetworkedInventory>()) { inventory }
        itemHolder = StaticVanillaItemHolder(this, inventories)
    }
    
}

class VanillaChestTileEntity(chest: Chest) : ItemStorageVanillaTileEntity(chest) {
    
    private lateinit var inventories: EnumMap<BlockFace, NetworkedInventory>
    override val itemHolder: ItemHolder
    
    init {
        setInventories()
        itemHolder = DynamicVanillaItemHolder(this) { inventories }
    }
    
    private fun setInventories() {
        val inventory = NetworkedBukkitInventory((block.state as Chest).inventory)
        inventories = CUBE_FACES.associateWithTo(EnumMap(BlockFace::class.java)) { inventory }
    }
    
    fun handleChestStateChange() {
        setInventories()
        NetworkManager.runAsync {
            it.handleEndPointRemove(this, true)
            it.handleEndPointAdd(this, false)
        }
    }
    
}

class VanillaFurnaceTileEntity(furnace: Furnace) : ItemStorageVanillaTileEntity(furnace) {
    
    override val itemHolder = StaticVanillaItemHolder(this, getInventories(furnace))
    
    private fun getInventories(furnace: Furnace): EnumMap<BlockFace, NetworkedInventory> {
        val bukkitInventory = furnace.inventory
        val inputInventory = NetworkedRangedBukkitInventory(bukkitInventory, 0)
        val fuelInventory = NetworkedRangedBukkitInventory(bukkitInventory, 1)
        val outputInventory = NetworkedRangedBukkitInventory(bukkitInventory, 2)
        
        val inventories = CUBE_FACES.associateWithTo(emptyEnumMap<BlockFace, NetworkedInventory>()) { fuelInventory }
        inventories[BlockFace.UP] = inputInventory
        inventories[BlockFace.DOWN] = outputInventory
        
        return inventories
    }
    
}
