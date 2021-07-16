package xyz.xenondevs.nova.tileentity.impl

import com.google.common.base.Preconditions
import com.google.gson.JsonObject
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import org.bukkit.Axis
import org.bukkit.Bukkit
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Orientable
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.hitbox.Hitbox
import xyz.xenondevs.nova.item.impl.getFilterConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.Network
import xyz.xenondevs.nova.network.NetworkManager
import xyz.xenondevs.nova.network.NetworkNode
import xyz.xenondevs.nova.network.NetworkType
import xyz.xenondevs.nova.network.NetworkType.ENERGY
import xyz.xenondevs.nova.network.NetworkType.ITEMS
import xyz.xenondevs.nova.network.energy.EnergyBridge
import xyz.xenondevs.nova.network.item.ItemBridge
import xyz.xenondevs.nova.network.item.ItemConnectionType
import xyz.xenondevs.nova.network.item.ItemStorage
import xyz.xenondevs.nova.tileentity.Model
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.ui.CableItemConfigGUI
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.util.point.Point3D
import java.util.*

private const val CONNECTOR = 1
private const val HORIZONTAL = 2
private const val DOWN = 3
private const val UP = 4
private val ATTACHMENTS: IntArray = (5..13).toIntArray()

private val SUPPORTED_NETWORK_TYPES = arrayOf(ENERGY, ITEMS)

open class Cable(
    override val energyTransferRate: Int,
    override val itemTransferRate: Int,
    ownerUUID: UUID?,
    material: NovaMaterial,
    data: JsonObject,
    armorStand: ArmorStand
) : TileEntity(
    ownerUUID,
    material,
    data,
    armorStand,
), EnergyBridge, ItemBridge {
    
    override val networks = EnumMap<NetworkType, Network>(NetworkType::class.java)
    override val bridgeFaces = retrieveData("bridgeFaces") { CUBE_FACES.toMutableSet() }
    
    override val gui: TileEntityGUI? = null
    
    override val connectedNodes: MutableMap<NetworkType, MutableMap<BlockFace, NetworkNode>> =
        NetworkType.values().associateWithTo(emptyEnumMap()) { enumMapOf() }
    
    private var filterInventoriesInitialized = false
    private val filterInventories by lazy {
        filterInventoriesInitialized = true
        
        mapOf(
            ItemConnectionType.INSERT to
                CUBE_FACES.associateWith { getInventory("filter_insert_$it", 1, true, ::handleFilterInventoryUpdate) },
            ItemConnectionType.EXTRACT to
                CUBE_FACES.associateWith { getInventory("filter_extract_$it", 1, true, ::handleFilterInventoryUpdate) }
        )
    }
    
    private val multiModel = getMultiModel("cableModels")
    private val hitboxes = ArrayList<Hitbox>()
    
    override fun saveData() {
        super.saveData()
        storeData("bridgeFaces", bridgeFaces)
    }
    
    override fun handleNetworkUpdate() {
        if (isValid) {
            if (NOVA.isEnabled) {
                multiModel.replaceModels(getModelsNeeded())
                updateHitbox()
            }
        }
    }
    
    override fun handleInitialized(first: Boolean) {
        NetworkManager.handleBridgeAdd(this, *SUPPORTED_NETWORK_TYPES)
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        NetworkManager.handleBridgeRemove(this, unload)
        hitboxes.forEach { it.remove() }
        
        if (!unload && filterInventoriesInitialized) {
            filterInventories.values
                .flatMap { it.values.toList() }
                .flatMap { it.windows }
                .mapNotNull { it.currentViewer }
                .forEach(Player::closeInventory)
        }
    }
    
    override fun getFilter(type: ItemConnectionType, blockFace: BlockFace) =
        filterInventories[type]?.get(blockFace)?.getItemStack(0)?.getFilterConfig()
    
    private fun handleFilterInventoryUpdate(event: ItemUpdateEvent) {
        if (event.newItemStack != null && event.newItemStack?.novaMaterial != NovaMaterial.ITEM_FILTER)
            event.isCancelled = true
    }
    
    private fun getModelsNeeded(): List<Model> {
        Preconditions.checkState(networks.isNotEmpty(), "No network is initialized")
        
        val items = ArrayList<Pair<ItemStack, Float>>()
        
        val connectedFaces = connectedNodes.values.flatMapTo(HashSet()) { it.keys }
        
        // only show connector if connections aren't on two opposite sides
        if (connectedFaces.size != 2 || connectedFaces.first() != connectedFaces.last().oppositeFace) {
            items += material.block!!.getItem(CONNECTOR) to 0f
        }
        
        // add all cable connections
        connectedFaces.forEach { blockFace ->
            val dataIndex = when (blockFace) {
                BlockFace.DOWN -> DOWN
                BlockFace.UP -> UP
                else -> HORIZONTAL
            }
            
            val itemStack = material.block!!.getItem(dataIndex)
            items += itemStack to getRotation(blockFace)
        }
        
        // add all item network attachments
        connectedNodes[ITEMS]!!
            .filter { it.value is ItemStorage }
            .forEach { (blockFace, itemStorage) ->
                itemStorage as ItemStorage
                
                val attachmentIndex = when (itemStorage.itemConfig[blockFace.oppositeFace] ?: ItemConnectionType.NONE) {
                    ItemConnectionType.INSERT -> 0
                    ItemConnectionType.EXTRACT -> 1
                    ItemConnectionType.BUFFER -> 2
                    else -> throw UnsupportedOperationException()
                } * 3 + when (blockFace) {
                    BlockFace.DOWN -> 1
                    BlockFace.UP -> 2
                    else -> 0
                }
                
                val itemStack = material.block!!.getItem(ATTACHMENTS[attachmentIndex])
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
            val pointA: Point3D
            val pointB: Point3D
            if (connectedNodes.values.any { it.containsKey(blockFace) }) {
                pointA = Point3D(0.3, 0.3, 0.0)
                pointB = Point3D(0.7, 0.7, 0.5)
            } else {
                pointA = Point3D(0.3, 0.3, 0.3)
                pointB = Point3D(0.7, 0.7, 0.5)
            }
            
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
                { it.action.isRightClick() && it.hasItem() && it.item!!.novaMaterial == NovaMaterial.WRENCH },
                { handleCableWrenchHit(it, blockFace) }
            )
        }
    }
    
    private fun createAttachmentHitboxes() {
        val neighborEndPoints = connectedNodes
            .values
            .flatMap { it.entries }
            .filter { (blockFace, node) -> node is ItemStorage && node.itemConfig[blockFace.oppositeFace] != ItemConnectionType.NONE }
            .associate { it.key to it.value as ItemStorage }
        
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
        val neighborFaces = connectedNodes.flatMapTo(HashSet()) { it.value.keys }
        val axis = when {
            neighborFaces.contains(BlockFace.EAST) && neighborFaces.contains(BlockFace.WEST) -> Axis.X
            neighborFaces.contains(BlockFace.NORTH) && neighborFaces.contains(BlockFace.SOUTH) -> Axis.Z
            neighborFaces.contains(BlockFace.UP) && neighborFaces.contains(BlockFace.DOWN) -> Axis.Y
            else -> {
                connectedNodes.values
                    .mapNotNull { faceMap -> faceMap.keys.firstOrNull() }
                    .firstOrNull()
                    ?.axis
                    ?: Axis.X
            }
        }
        
        // run later because this might be called during an event
        runTaskLater(1) {
            if (isValid) {
                val block = armorStand.location.block
                val blockData = block.blockData as Orientable
                blockData.axis = axis
                block.setBlockData(blockData, false)
            }
        }
    }
    
    private fun handleAttachmentHit(event: PlayerInteractEvent, face: BlockFace, itemStorage: ItemStorage) {
        event.isCancelled = true
        CableItemConfigGUI(
            itemStorage,
            face.oppositeFace,
            filterInventories[ItemConnectionType.INSERT]!![face]!!,
            filterInventories[ItemConnectionType.EXTRACT]!![face]!!
        ).openWindow(event.player)
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

class BasicCable(
    ownerUUID: UUID?,
    material: NovaMaterial,
    data: JsonObject,
    armorStand: ArmorStand
) : Cable(
    NovaConfig.getInt("cable.basic.energy_transfer_rate")!!,
    NovaConfig.getInt("cable.basic.item_transfer_rate")!!,
    ownerUUID,
    material,
    data,
    armorStand
)

class AdvancedCable(
    ownerUUID: UUID?,
    material: NovaMaterial,
    data: JsonObject,
    armorStand: ArmorStand
) : Cable(
    NovaConfig.getInt("cable.advanced.energy_transfer_rate")!!,
    NovaConfig.getInt("cable.advanced.item_transfer_rate")!!,
    ownerUUID,
    material,
    data,
    armorStand
)

class EliteCable(
    ownerUUID: UUID?,
    material: NovaMaterial,
    data: JsonObject,
    armorStand: ArmorStand
) : Cable(
    NovaConfig.getInt("cable.elite.energy_transfer_rate")!!,
    NovaConfig.getInt("cable.elite.item_transfer_rate")!!,
    ownerUUID,
    material,
    data,
    armorStand
)

class UltimateCable(
    ownerUUID: UUID?,
    material: NovaMaterial,
    data: JsonObject,
    armorStand: ArmorStand
) : Cable(
    NovaConfig.getInt("cable.ultimate.energy_transfer_rate")!!,
    NovaConfig.getInt("cable.ultimate.item_transfer_rate")!!,
    ownerUUID,
    material,
    data,
    armorStand
)

class CreativeCable(
    ownerUUID: UUID?,
    material: NovaMaterial,
    data: JsonObject,
    armorStand: ArmorStand
) : Cable(
    Int.MAX_VALUE,
    Int.MAX_VALUE,
    ownerUUID,
    material,
    data,
    armorStand
)
