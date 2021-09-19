package xyz.xenondevs.nova.tileentity.impl.energy

import com.google.common.base.Preconditions
import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.virtualinventory.VirtualInventoryManager
import org.bukkit.Axis
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Orientable
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.item.impl.getFilterConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.Model
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.tileentity.network.*
import xyz.xenondevs.nova.tileentity.network.NetworkType.ENERGY
import xyz.xenondevs.nova.tileentity.network.NetworkType.ITEMS
import xyz.xenondevs.nova.tileentity.network.energy.EnergyBridge
import xyz.xenondevs.nova.tileentity.network.item.ItemBridge
import xyz.xenondevs.nova.tileentity.network.item.ItemConnectionType
import xyz.xenondevs.nova.tileentity.network.item.ItemFilter
import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.ui.CableItemConfigGUI
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.util.data.plus
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import xyz.xenondevs.nova.world.hitbox.Hitbox
import xyz.xenondevs.nova.world.point.Point3D
import java.util.*

private val ATTACHMENTS: IntArray = (64..72).toIntArray()

private val SUPPORTED_NETWORK_TYPES = arrayOf(ENERGY, ITEMS)

private val NetworkNode.itemHolder: ItemHolder?
    get() = if (this is NetworkEndPoint && holders.contains(ITEMS)) holders[ITEMS] as ItemHolder else null

open class Cable(
    override val energyTransferRate: Int,
    override val itemTransferRate: Int,
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : TileEntity(uuid, data, material, ownerUUID, armorStand), EnergyBridge, ItemBridge {
    
    override val networks = EnumMap<NetworkType, Network>(NetworkType::class.java)
    override val bridgeFaces = retrieveEnumCollectionOrNull("bridgeFaces", HashSet()) ?: CUBE_FACES.toMutableSet()
    
    override val gui: Lazy<TileEntityGUI>? = null
    
    override val connectedNodes: MutableMap<NetworkType, MutableMap<BlockFace, NetworkNode>> =
        NetworkType.values().associateWithTo(emptyEnumMap()) { enumMapOf() }
    
    private val insertFilters: MutableMap<BlockFace, ItemFilter> = retrieveFilterMap("insertFilters")
    private val extractFilters: MutableMap<BlockFace, ItemFilter> = retrieveFilterMap("extractFilters")
    private val configGUIs = emptyEnumMap<BlockFace, CableItemConfigGUI>()
    
    private val multiModel = createMultiModel()
    private val hitboxes = ArrayList<Hitbox>()
    
    init {
        // Convert legacy virtual inventories holding the filter items to ItemFilter configs
        val manager = VirtualInventoryManager.getInstance()
        fun VirtualInventory.toFilterConfig(): ItemFilter? = getItemStack(0)?.getFilterConfig()
        
        CUBE_FACES.associateWithTo(emptyEnumMap()) {
            manager.getByUuid(uuid.salt("filter_insert_$it"))
                ?.also(manager::remove)
                ?.toFilterConfig()
        }.forEach { (face, itemFilter) ->
            if (itemFilter != null) insertFilters[face] = itemFilter
        }
        
        CUBE_FACES.associateWithTo(emptyEnumMap()) {
            manager.getByUuid(uuid.salt("filter_extract_$it"))
                ?.also(manager::remove)
                ?.toFilterConfig()
        }.forEach { (face, itemFilter) ->
            if (itemFilter != null) extractFilters[face] = itemFilter
        }
    }
    
    private fun retrieveFilterMap(name: String) =
        retrieveEnumMapOrNull<BlockFace, CompoundElement>(name)
            ?.mapValuesTo(emptyEnumMap()) { ItemFilter(it.value) }
            ?: emptyEnumMap()
    
    override fun saveData() {
        super.saveData()
        storeList("bridgeFaces", bridgeFaces)
        storeEnumMap("insertFilters", insertFilters.mapValues { it.value.compound })
        storeEnumMap("extractFilters", extractFilters.mapValues { it.value.compound })
    }
    
    override fun handleNetworkUpdate() {
        // assume that we have NetworkManager.LOCK
        if (isValid && NOVA.isEnabled) {
            multiModel.replaceModels(getModelsNeeded())
            updateHeadStack()
            
            configGUIs.forEach { (face, gui) ->
                if (gui != null) {
                    val neighbor = connectedNodes[ITEMS]?.get(face)
                    if (neighbor is NetworkEndPoint && neighbor.holders.contains(ITEMS)) {
                        val itemHolder = neighbor.holders[ITEMS] as ItemHolder
                        gui.itemHolder = itemHolder
                        gui.updateButtons()
                    } else gui.itemHolder = null
                }
            }
            
            // !! Needs to be run in the server thread (updating blocks)
            // !! Also needs to be synchronized with NetworkManager as connectedNodes are retrieved
            // This is not using a simple synchronized call as it would (in the worst case) block the main thread
            // until all other tasks accessing the NetworkManager have run, defeating the purpose of doing this async.
            runSyncTaskWhenUnlocked(NetworkManager.LOCK, ::updateHitbox)
        }
    }
    
    override fun handleInitialized(first: Boolean) {
        runAsyncTask { NetworkManager.handleBridgeAdd(this, *SUPPORTED_NETWORK_TYPES) }
    }
    
    override fun handleHitboxPlaced() {
        updateBlockHitbox()
    }
    
    override fun destroy(dropItems: Boolean): ArrayList<ItemStack> {
        val items = super.destroy(dropItems)
        if (dropItems) {
            (extractFilters.values.stream() + insertFilters.values.stream())
                .map { it.createFilterItem() }
                .forEach(items::add)
        }
        
        return items
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        
        val task = { NetworkManager.handleBridgeRemove(this, unload) }
        if (NOVA.isEnabled) runAsyncTask(task) else task()
        
        hitboxes.forEach { it.remove() }
        
        if (!unload) configGUIs.values.forEach(CableItemConfigGUI::closeForAllViewers)
    }
    
    override fun getFilter(type: ItemConnectionType, blockFace: BlockFace) =
        when (type) {
            ItemConnectionType.INSERT -> insertFilters[blockFace]
            ItemConnectionType.EXTRACT -> extractFilters[blockFace]
            else -> null
        }
    
    override fun getHeadStack(): ItemStack {
        val connectedFaces = connectedNodes.values.flatMapTo(HashSet()) { it.keys }
        
        val booleans = CUBE_FACES.map { connectedFaces.contains(it) }.reversed().toBooleanArray()
        val number = MathUtils.convertBooleanArrayToInt(booleans)
        return material.block!!.createItemStack(number)
    }
    
    private fun getModelsNeeded(): List<Model> {
        Preconditions.checkState(networks.isNotEmpty(), "No network is initialized")
        
        val items = ArrayList<Pair<ItemStack, Float>>()
        
        connectedNodes[ITEMS]!!
            .filter { val node = it.value; node is NetworkEndPoint && node.holders.contains(ITEMS) }
            .forEach { (blockFace, node) ->
                val itemHolder = (node as NetworkEndPoint).holders[ITEMS] as ItemHolder
                
                val attachmentIndex = when (itemHolder.itemConfig[blockFace.oppositeFace]
                    ?: ItemConnectionType.NONE) {
                    ItemConnectionType.INSERT -> 0
                    ItemConnectionType.EXTRACT -> 1
                    ItemConnectionType.BUFFER -> 2
                    else -> throw UnsupportedOperationException()
                } * 3 + when (blockFace) {
                    BlockFace.DOWN -> 1
                    BlockFace.UP -> 2
                    else -> 0
                }
                
                val itemStack = material.block!!.createItemStack(ATTACHMENTS[attachmentIndex])
                items += itemStack to getRotation(blockFace)
            }
        
        return items.map { Model(it.first, location.clone().center().apply { yaw = it.second }) }
    }
    
    private fun getRotation(blockFace: BlockFace) =
        when (blockFace) {
            BlockFace.NORTH -> 0f
            BlockFace.EAST -> 90f
            BlockFace.SOUTH -> 180f
            BlockFace.WEST -> 270f
            else -> 0f
        }
    
    private fun updateHitbox() {
        updateVirtualHitboxes()
        updateBlockHitbox()
    }
    
    private fun updateVirtualHitboxes() {
        hitboxes.forEach { it.remove() }
        hitboxes.clear()
        
        createCableHitboxes()
        createAttachmentHitboxes()
    }
    
    private fun createCableHitboxes() {
        CUBE_FACES.forEach { blockFace ->
            val pointA = Point3D(0.3, 0.3, 0.0)
            val pointB = Point3D(0.7, 0.7, 0.5)
            
            val origin = Point3D(0.5, 0.5, 0.5)
            
            val rotationValues = blockFace.rotationValues
            pointA.rotateAroundXAxis(rotationValues.first, origin)
            pointA.rotateAroundYAxis(rotationValues.second, origin)
            pointB.rotateAroundXAxis(rotationValues.first, origin)
            pointB.rotateAroundYAxis(rotationValues.second, origin)
            
            val sortedPoints = Point3D.sort(pointA, pointB)
            val from = location.clone().add(sortedPoints.first.x, sortedPoints.first.y, sortedPoints.first.z)
            val to = location.clone().add(sortedPoints.second.x, sortedPoints.second.y, sortedPoints.second.z)
            
            hitboxes += Hitbox(
                from, to,
                { it.action.isRightClick() && it.hasItem() && it.item!!.novaMaterial == NovaMaterialRegistry.WRENCH },
                { handleCableWrenchHit(it, blockFace) }
            )
        }
    }
    
    private fun createAttachmentHitboxes() {
        val neighborEndPoints = connectedNodes
            .values
            .flatMap { it.entries }
            .filter { (blockFace, node) ->
                val itemHolder = node.itemHolder
                itemHolder != null && itemHolder.itemConfig[blockFace.oppositeFace] != ItemConnectionType.NONE
            }
            .associate { it.key to it.value.itemHolder!! }
        
        neighborEndPoints
            .map { it.key }
            .forEach { blockFace ->
                val pointA = Point3D(0.125, 0.125, 0.0)
                val pointB = Point3D(0.875, 0.875, 0.2)
                
                val origin = Point3D(0.5, 0.5, 0.5)
                
                val rotationValues = blockFace.rotationValues
                pointA.rotateAroundXAxis(rotationValues.first, origin)
                pointA.rotateAroundYAxis(rotationValues.second, origin)
                pointB.rotateAroundXAxis(rotationValues.first, origin)
                pointB.rotateAroundYAxis(rotationValues.second, origin)
                
                val sortedPoints = Point3D.sort(pointA, pointB)
                val from = location.clone().add(sortedPoints.first.x, sortedPoints.first.y, sortedPoints.first.z)
                val to = location.clone().add(sortedPoints.second.x, sortedPoints.second.y, sortedPoints.second.z)
                
                hitboxes += Hitbox(from, to) { handleAttachmentHit(it, blockFace, neighborEndPoints[blockFace]!!) }
            }
    }
    
    private fun updateBlockHitbox() {
        val block = location.block
        
        val neighborFaces = connectedNodes.flatMapTo(HashSet()) { it.value.keys }
        val axis = when {
            neighborFaces.contains(BlockFace.EAST) && neighborFaces.contains(BlockFace.WEST) -> Axis.X
            neighborFaces.contains(BlockFace.NORTH) && neighborFaces.contains(BlockFace.SOUTH) -> Axis.Z
            neighborFaces.contains(BlockFace.UP) && neighborFaces.contains(BlockFace.DOWN) -> Axis.Y
            else -> null
        }
        
        if (axis != null) {
            block.type = Material.CHAIN
            val blockData = block.blockData as Orientable
            blockData.axis = axis
            block.setBlockData(blockData, false)
        } else {
            block.type = Material.STRUCTURE_VOID
        }
    }
    
    private fun handleAttachmentHit(event: PlayerInteractEvent, face: BlockFace, itemHolder: ItemHolder) {
        event.isCancelled = true
        configGUIs.getOrPut(face) { createAttachmentGUI(face, itemHolder) }.openWindow(event.player)
    }
    
    private fun createAttachmentGUI(face: BlockFace, itemHolder: ItemHolder) =
        CableItemConfigGUI(
            itemHolder,
            face.oppositeFace,
            createFilterInventory(face, ItemConnectionType.INSERT),
            createFilterInventory(face, ItemConnectionType.EXTRACT)
        )
    
    private fun createFilterInventory(face: BlockFace, type: ItemConnectionType): VirtualInventory {
        val map = when (type) {
            ItemConnectionType.INSERT -> insertFilters
            ItemConnectionType.EXTRACT -> extractFilters
            else -> throw UnsupportedOperationException()
        }
        
        val filterItem = map[face]?.let(ItemFilter::createFilterItem)
        val inventory = VirtualInventory(null, 1, arrayOf(filterItem), intArrayOf(1))
        
        inventory.setItemUpdateHandler {
            if (it.newItemStack != null && it.newItemStack?.novaMaterial != NovaMaterialRegistry.ITEM_FILTER) {
                it.isCancelled = true
                return@setItemUpdateHandler
            }
            
            val filterConfig = it.newItemStack?.getFilterConfig()
            if (filterConfig != null) map[face] = filterConfig
            else map.remove(face)
        }
        
        return inventory
    }
    
    private fun handleCableWrenchHit(event: PlayerInteractEvent, face: BlockFace) {
        event.isCancelled = true
        
        val player = event.player
        if (player.isSneaking) {
            Bukkit.getPluginManager().callEvent(BlockBreakEvent(location.block, player))
        } else {
            if (connectedNodes.values.any { it.containsKey(face) }) {
                NetworkManager.handleBridgeRemove(this, false)
                bridgeFaces.remove(face)
                NetworkManager.handleBridgeAdd(this, *SUPPORTED_NETWORK_TYPES)
            } else if (!bridgeFaces.contains(face)) {
                NetworkManager.handleBridgeRemove(this, false)
                bridgeFaces.add(face)
                NetworkManager.handleBridgeAdd(this, *SUPPORTED_NETWORK_TYPES)
            }
        }
    }
    
    override fun handleTick() = Unit
    
}

private val BASIC_ENERGY_RATE = NovaConfig[NovaMaterialRegistry.BASIC_CABLE].getInt("energy_transfer_rate")!!
private val BASIC_ITEM_RATE = NovaConfig[NovaMaterialRegistry.BASIC_CABLE].getInt("energy_transfer_rate")!!

private val ADVANCED_ENERGY_RATE = NovaConfig[NovaMaterialRegistry.ADVANCED_CABLE].getInt("energy_transfer_rate")!!
private val ADVANCED_ITEM_RATE = NovaConfig[NovaMaterialRegistry.ADVANCED_CABLE].getInt("energy_transfer_rate")!!

private val ELITE_ENERGY_RATE = NovaConfig[NovaMaterialRegistry.ELITE_CABLE].getInt("energy_transfer_rate")!!
private val ELITE_ITEM_RATE = NovaConfig[NovaMaterialRegistry.ELITE_CABLE].getInt("energy_transfer_rate")!!

private val ULTIMATE_ENERGY_RATE = NovaConfig[NovaMaterialRegistry.ULTIMATE_CABLE].getInt("energy_transfer_rate")!!
private val ULTIMATE_ITEM_RATE = NovaConfig[NovaMaterialRegistry.ULTIMATE_CABLE].getInt("energy_transfer_rate")!!

class BasicCable(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : Cable(
    BASIC_ENERGY_RATE,
    BASIC_ITEM_RATE,
    uuid,
    data,
    material,
    ownerUUID,
    armorStand,
)

class AdvancedCable(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : Cable(
    ADVANCED_ENERGY_RATE,
    ADVANCED_ITEM_RATE,
    uuid,
    data,
    material,
    ownerUUID,
    armorStand,
)

class EliteCable(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : Cable(
    ELITE_ENERGY_RATE,
    ELITE_ITEM_RATE,
    uuid,
    data,
    material,
    ownerUUID,
    armorStand,
)

class UltimateCable(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : Cable(
    ULTIMATE_ENERGY_RATE,
    ULTIMATE_ITEM_RATE,
    uuid,
    data,
    material,
    ownerUUID,
    armorStand,
)

class CreativeCable(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : Cable(
    Int.MAX_VALUE,
    Int.MAX_VALUE,
    uuid,
    data,
    material,
    ownerUUID,
    armorStand,
)
