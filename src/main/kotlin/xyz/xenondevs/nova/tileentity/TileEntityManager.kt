package xyz.xenondevs.nova.tileentity

import org.bukkit.*
import org.bukkit.entity.ArmorStand
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.serialization.TileEntitySerialization
import xyz.xenondevs.nova.tileentity.serialization.UUIDDataType
import xyz.xenondevs.nova.util.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.roundToInt

val UUID_KEY = NamespacedKey(NOVA, "tileEntityUUID")
private val TILE_ENTITY_KEY = NamespacedKey(NOVA, "tileEntity")

fun ItemStack.addUUID(uuid: UUID) {
    if (hasItemMeta()) {
        val itemMeta = this.itemMeta!!
        val dataContainer = itemMeta.persistentDataContainer
        dataContainer.set(UUID_KEY, UUIDDataType, uuid)
        this.itemMeta = itemMeta
    }
}

fun ItemStack.getUUID(): UUID? {
    if (hasItemMeta()) {
        val dataContainer = itemMeta!!.persistentDataContainer
        if (dataContainer.has(UUID_KEY, UUIDDataType)) {
            return dataContainer.get(UUID_KEY, UUIDDataType)
        }
    }
    
    return null
}

fun ItemStack.hasUUID(): Boolean {
    if (hasItemMeta()) {
        val dataContainer = itemMeta!!.persistentDataContainer
        return dataContainer.has(UUID_KEY, UUIDDataType)
    }
    
    return false
}

object TileEntityManager : Listener {
    
    private val tileEntityMap = HashMap<Chunk, HashMap<Location, TileEntity>>()
    private val tileEntities: List<TileEntity>
        get() = tileEntityMap.flatMap { (_, chunkMap) -> chunkMap.values }
    
    init {
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
        Bukkit.getWorlds().flatMap { it.loadedChunks.asList() }.forEach(this::handleChunkLoad)
        NOVA.disableHandlers += { Bukkit.getWorlds().flatMap { it.loadedChunks.asList() }.forEach(this::handleChunkUnload) }
        runTaskTimer(0, 1) { tileEntities.forEach(TileEntity::handleTick) }
    }
    
    fun placeTileEntity(location: Location, rotation: Float, material: NovaMaterial, uuid: UUID?) {
        val block = location.block
        
        // spawn ArmorStand there
        val headItem = material.block!!.getItem("")
        val spawnLocation = location
            .clone()
            .add(0.5, 0.0, 0.5)
            .also {
                var yaw = rotation % 360
                if (yaw < 0) yaw += 360
                yaw = (yaw / 90).roundToInt() * 90f
                yaw += 180
                it.yaw = yaw
            }
        val armorStand = EntityUtils.spawnArmorStandSilently(
            spawnLocation,
            headItem
        )
        
        // create TileEntity instance
        val tileEntity = material.tileEntityConstructor!!(material, uuid ?: UUID.randomUUID(), armorStand)
        
        // save identifying TileEntity data to ArmorStand
        armorStand.persistentDataContainer.set(
            TILE_ENTITY_KEY,
            PersistentDataType.STRING,
            TileEntitySerialization.serialize(tileEntity)
        )
        
        // add to tileEntities map
        val chunk = block.chunk
        val chunkMap = tileEntityMap[chunk] ?: HashMap<Location, TileEntity>().also { tileEntityMap[chunk] = it }
        chunkMap[location] = tileEntity
        
        // set hitbox block there (1 tick later or it collides with the cancelled event which removes the block)
        runTaskLater(1) { block.type = material.hitbox!! }
    }
    
    fun destroyTileEntity(tileEntity: TileEntity, dropItems: Boolean) {
        val location = tileEntity.armorStand.location.clone().subtract(0.5, 0.0, 0.5)
        val chunk = location.chunk
        
        location.block.type = Material.AIR
        
        val drops = tileEntity.destroy(dropItems) // destroy tileEntity and save drops for later
        
        // remove TileEntity and ArmorStand
        tileEntityMap[chunk]?.remove(location)
        tileEntity.armorStand.remove()
        
        // drop items a tick later to prevent interference with the cancellation of the break event
        runTaskLater(1) { location.clone().dropItems(drops) }
    }
    
    fun getTileEntityAt(location: Location) = tileEntityMap[location.chunk]?.get(location)
    
    private fun handleChunkLoad(chunk: Chunk) {
        val chunkMap = HashMap<Location, TileEntity>()
        
        chunk.entities
            .filterIsInstance<ArmorStand>()
            .forEach { armorStand ->
                val dataContainer = armorStand.persistentDataContainer
                if (dataContainer.has(TILE_ENTITY_KEY, PersistentDataType.STRING)) {
                    val data = dataContainer.get(TILE_ENTITY_KEY, PersistentDataType.STRING)!!
                    val tileEntity = TileEntitySerialization.deserialize(armorStand, data)
                    chunkMap[armorStand.location.clone().subtract(0.5, 0.0, 0.5)] = tileEntity
                }
            }
        
        tileEntityMap[chunk] = chunkMap
    }
    
    private fun handleChunkUnload(chunk: Chunk) {
        tileEntityMap[chunk]?.forEach { (_, tileEntity) -> tileEntity.handleDisable() }
        tileEntityMap.remove(chunk)
    }
    
    @EventHandler
    fun handleChunkLoad(event: ChunkLoadEvent) {
        handleChunkLoad(event.chunk)
    }
    
    @EventHandler
    fun handleChunkUnload(event: ChunkUnloadEvent) {
        handleChunkUnload(event.chunk)
    }
    
    @EventHandler
    fun handlePlace(event: BlockPlaceEvent) {
        val player = event.player
        val placedItem = event.itemInHand
        val material = NovaMaterial.toNovaMaterial(placedItem)
        if (material != null) {
            event.isCancelled = true
            if (material.isBlock) {
                placeTileEntity(event.block.location, player.location.yaw, material, placedItem.getUUID())
                
                if (player.gameMode == GameMode.SURVIVAL) placedItem.amount--
            }
        }
    }
    
    @EventHandler
    fun handleBreak(event: BlockBreakEvent) {
        val tileEntity = getTileEntityAt(event.block.location)
        if (tileEntity != null) {
            event.isCancelled = true
            destroyTileEntity(tileEntity, event.player.gameMode == GameMode.SURVIVAL)
        }
    }
    
    @EventHandler
    fun handleInteract(event: PlayerInteractEvent) {
        val action = event.action
        if (action == Action.RIGHT_CLICK_BLOCK && !event.player.isSneaking) {
            val block = event.clickedBlock!!
            val tileEntity = getTileEntityAt(block.location)
            if (tileEntity != null) {
                event.isCancelled = true
                tileEntity.handleRightClick(event)
            }
        }
    }
    
}
