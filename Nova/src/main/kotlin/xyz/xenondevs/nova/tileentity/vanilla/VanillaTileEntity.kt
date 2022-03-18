package xyz.xenondevs.nova.tileentity.vanilla

import org.bukkit.Location
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
import xyz.xenondevs.nova.tileentity.network.item.inventory.*
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.data.HashUtils
import xyz.xenondevs.nova.util.emptyEnumMap
import xyz.xenondevs.nova.util.enumMapOf
import xyz.xenondevs.nova.util.runTaskLaterSynchronized
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
    
    abstract fun saveData()
    
    fun writeToDataContainer() {
        val tileState = block.state
        if (tileState is TileState) {
            tileState.persistentDataContainer.set(TILE_ENTITY_KEY, CompoundElementDataType, data)
            tileState.update()
        }
    }
    
}

abstract class ItemStorageVanillaTileEntity(tileState: TileState) : VanillaTileEntity(tileState), NetworkEndPoint {
    
    abstract val itemHolder: ItemHolder
    override val location = tileState.location
    override val uuid = HashUtils.getUUID(tileState.location)
    
    final override val networks: MutableMap<NetworkType, MutableMap<BlockFace, Network>> = emptyEnumMap()
    final override val connectedNodes: MutableMap<NetworkType, MutableMap<BlockFace, NetworkNode>> = emptyEnumMap()
    final override val holders: MutableMap<NetworkType, EndPointDataHolder>
        by lazy { enumMapOf(NetworkType.ITEMS to itemHolder) }
    
    override fun handleInitialized() {
        NetworkManager.queueAsync { it.addEndPoint(this) }
    }
    
    override fun handleRemoved(unload: Boolean) {
        if (!unload) NetworkManager.queueAsync { it.removeEndPoint(this) }
    }
    
    override fun retrieveSerializedNetworks(): Map<NetworkType, Map<BlockFace, UUID>>? =
        retrieveEnumMapOrNull("networks")
    
    override fun retrieveSerializedConnectedNodes(): Map<NetworkType, Map<BlockFace, UUID>>? =
        retrieveEnumMapOrNull("connectedNodes")
    
    override fun saveData() {
        storeEnumMap("networks", serializeNetworks())
        storeEnumMap("connectedNodes", serializeConnectedNodes())
        itemHolder.saveData()
    }
    
}

class VanillaContainerTileEntity(container: Container) : ItemStorageVanillaTileEntity(container) {
    
    override val itemHolder: StaticVanillaItemHolder
    
    init {
        val inventory = if (container is ShulkerBox) NetworkedShulkerBoxInventory(container.inventory) else NetworkedBukkitInventory(container.inventory)
        val inventories = CUBE_FACES.associateWithTo(emptyEnumMap<BlockFace, NetworkedInventory>()) { inventory }
        itemHolder = StaticVanillaItemHolder(this, inventories)
    }
    
}

class VanillaChestTileEntity(chest: Chest) : ItemStorageVanillaTileEntity(chest) {
    
    private lateinit var inventories: EnumMap<BlockFace, NetworkedInventory>
    private lateinit var allowedConnectionTypes: HashMap<NetworkedInventory, NetworkConnectionType>
    override val itemHolder: ItemHolder
    
    private var initialized = false
    private var doubleChestLocation: Location? = null
    
    init {
        setInventories()
        itemHolder = DynamicVanillaItemHolder(this, { inventories }, { allowedConnectionTypes })
        
        runTaskLaterSynchronized(VanillaTileEntityManager, 1) {
            doubleChestLocation = getOtherChestLocation()
            doubleChestLocation?.let {
                val tileEntity = VanillaTileEntityManager.getTileEntityAt(it)
                if (tileEntity is VanillaChestTileEntity) tileEntity.handleChestStateChange()
            }
            handleChestStateChange()
        }
    }
    
    // Should not be added to the NetworkManager before checking if it's a double chest
    override fun handleInitialized() = Unit
    
    private fun setInventories() {
        val chest = block.state
        if (chest is Chest) {
            val inventory = NetworkedChestInventory(chest.inventory)
            inventories = CUBE_FACES.associateWithTo(emptyEnumMap()) { inventory }
            allowedConnectionTypes = inventories.entries.associateTo(HashMap()) { (_, inv) -> inv to NetworkConnectionType.BUFFER }
        }
    }
    
    private fun getOtherChestLocation(): Location? {
        val chest = block.state
        if (chest is Chest) {
            val holder = chest.inventory.holder
            
            if (holder is DoubleChest) {
                val left = holder.leftSide as Chest
                val right = holder.rightSide as Chest
                
                return if (left.location == location) right.location else left.location
            }
        }
        
        return null
    }
    
    fun handleChestStateChange() {
        setInventories()
        NetworkManager.queueAsync {
            it.removeEndPoint(this, false)
            it.addEndPoint(this, false).thenRun {
                if (!initialized) {
                    initialized = true
                    updateNearbyBridges()
                }
            }
        }
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        
        if (NOVA.isEnabled) {
            val doubleChestLocation = doubleChestLocation
            if (doubleChestLocation != null) {
                runTaskLaterSynchronized(VanillaTileEntityManager, 1) {
                    val chest = VanillaTileEntityManager.getTileEntityAt(doubleChestLocation)
                    if (chest is VanillaChestTileEntity) chest.handleChestStateChange()
                }
            }
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