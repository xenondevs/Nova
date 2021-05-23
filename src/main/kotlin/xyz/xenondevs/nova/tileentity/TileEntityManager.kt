package xyz.xenondevs.nova.tileentity

import com.google.gson.JsonObject
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.inventory.InventoryCreativeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.serialization.persistentdata.JsonElementDataType
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.util.protection.ProtectionUtils
import java.util.*
import kotlin.math.roundToInt

val TILE_ENTITY_KEY = NamespacedKey(NOVA, "tileEntity")

fun ItemStack.setTileEntityData(data: JsonObject) {
    if (hasItemMeta()) {
        val itemMeta = this.itemMeta!!
        val dataContainer = itemMeta.persistentDataContainer
        dataContainer.set(TILE_ENTITY_KEY, JsonElementDataType, data)
        this.itemMeta = itemMeta
    }
}

fun ItemStack.getTileEntityData(): JsonObject? {
    if (hasItemMeta()) {
        val dataContainer = itemMeta!!.persistentDataContainer
        if (dataContainer.has(TILE_ENTITY_KEY, JsonElementDataType)) {
            return dataContainer.get(TILE_ENTITY_KEY, JsonElementDataType) as JsonObject
        }
    }
    
    return null
}

fun ItemStack.hasTileEntityData(): Boolean {
    if (hasItemMeta()) {
        val dataContainer = itemMeta!!.persistentDataContainer
        return dataContainer.has(TILE_ENTITY_KEY, JsonElementDataType)
    }
    
    return false
}

fun ArmorStand.setTileEntityData(data: JsonObject) =
    persistentDataContainer.set(TILE_ENTITY_KEY, JsonElementDataType, data)

fun ArmorStand.getTileEntityData() =
    persistentDataContainer.get(TILE_ENTITY_KEY, JsonElementDataType)?.let { it as JsonObject }

fun ArmorStand.hasTileEntityData(): Boolean =
    persistentDataContainer.has(TILE_ENTITY_KEY, JsonElementDataType)

object TileEntityManager : Listener {
    
    private val tileEntityMap = HashMap<Chunk, HashMap<Location, TileEntity>>()
    private val locationCache = HashSet<Location>()
    val tileEntities: List<TileEntity>
        get() = tileEntityMap.flatMap { (_, chunkMap) -> chunkMap.values }
    
    fun init() {
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
        Bukkit.getWorlds().flatMap { it.loadedChunks.asList() }.forEach(this::handleChunkLoad)
        NOVA.disableHandlers += { Bukkit.getWorlds().flatMap { it.loadedChunks.asList() }.forEach(this::handleChunkUnload) }
        runTaskTimer(0, 1) { tileEntities.forEach(TileEntity::handleTick) }
        
        runTaskTimer(0, 1200) {
            // In some special cases no event is called when replacing a block. So we check for air blocks every minute.
            tileEntities.associateWith(TileEntity::location).forEach { (tileEntity, location) ->
                if (Material.AIR == location.block.type)
                    destroyTileEntity(tileEntity, false)
            }
        }
    }
    
    fun placeTileEntity(
        ownerUUID: UUID,
        location: Location,
        yaw: Float,
        material: NovaMaterial,
        data: JsonObject?
    ) {
        
        val block = location.block
        
        // spawn ArmorStand there
        val headItem = material.block!!.getItem("")
        val spawnLocation = location
            .clone()
            .add(0.5, 0.0, 0.5)
            .also { it.yaw = ((yaw + 180).mod(360f) / 90f).roundToInt() * 90f }
        val armorStand = EntityUtils.spawnArmorStandSilently(spawnLocation, headItem)
        
        // create TileEntity instance
        val tileEntity = material.createTileEntity!!(
            ownerUUID,
            material,
            data ?: JsonObject().apply { add("global", JsonObject()) },
            armorStand
        )
        
        // add to tileEntities map
        val chunk = block.chunk
        val chunkMap = tileEntityMap[chunk] ?: HashMap<Location, TileEntity>().also { tileEntityMap[chunk] = it }
        chunkMap[location] = tileEntity
        
        // add to location cache
        locationCache += location
        
        // set hitbox block a tick later to prevent interference with the cancellation of the BlockPlaceEvent
        runTaskLater(1) {
            if (material.hitbox != null) block.type = material.hitbox
            tileEntity.handleInitialized(true)
        }
    }
    
    fun destroyTileEntity(tileEntity: TileEntity, dropItems: Boolean): List<ItemStack> {
        val location = tileEntity.armorStand.location.blockLocation
        val chunk = location.chunk
        
        // remove TileEntity and ArmorStand
        tileEntityMap[chunk]?.remove(location)
        locationCache -= location
        tileEntity.armorStand.remove()
        
        location.block.type = Material.AIR
        val drops = tileEntity.destroy(dropItems) // destroy tileEntity and save drops for later
        
        tileEntity.handleRemoved(unload = false)
        
        return drops
    }
    
    fun destroyAndDropTileEntity(tileEntity: TileEntity, dropItems: Boolean) {
        val drops = destroyTileEntity(tileEntity, dropItems)
        
        // drop items a tick later to prevent interference with the cancellation of the break event
        runTaskLater(1) { tileEntity.location.dropItems(drops) }
    }
    
    fun getTileEntityAt(location: Location) = tileEntityMap[location.chunk]?.get(location)
    
    fun getTileEntitiesInChunk(chunk: Chunk) = tileEntityMap[chunk]?.values?.toList() ?: emptyList()
    
    private fun handleChunkLoad(chunk: Chunk) {
        val chunkMap = HashMap<Location, TileEntity>()
        
        chunk.entities
            .filterIsInstance<ArmorStand>()
            .filter(ArmorStand::hasTileEntityData)
            .forEach { armorStand ->
                armorStand.fireTicks = Int.MAX_VALUE
                val tileEntity = TileEntity.newInstance(armorStand)
                val location = armorStand.location
                    .clone()
                    .apply(Location::removeOrientation)
                    .subtract(0.5, 0.0, 0.5)
                chunkMap[location] = tileEntity
                locationCache += location
            }
        
        tileEntityMap[chunk] = chunkMap
        chunkMap.values.forEach { it.handleInitialized(false) }
    }
    
    private fun handleChunkUnload(chunk: Chunk) {
        val tileEntities = tileEntityMap[chunk]
        tileEntityMap -= chunk
        locationCache.removeAll { it.chunk == chunk }
        tileEntities?.forEach { (_, tileEntity) -> tileEntity.handleRemoved(unload = true) }
    }
    
    fun handleChunkLoad(event: ChunkLoadEvent) {
        handleChunkLoad(event.chunk)
    }
    
    fun handleChunkUnload(event: ChunkUnloadEvent) {
        handleChunkUnload(event.chunk)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handlePlace(event: BlockPlaceEvent) {
        val player = event.player
        val placedItem = event.itemInHand
        val material = placedItem.novaMaterial
        if (material != null) {
            event.isCancelled = true
            if (material.isBlock) {
                val location = event.block.location
                if (getTileEntityAt(location) == null) {
                    
                    placeTileEntity(
                        player.uniqueId,
                        event.block.location,
                        player.location.yaw,
                        material,
                        placedItem.getTileEntityData()
                    )
                    
                    if (player.gameMode == GameMode.SURVIVAL) placedItem.amount--
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handleBreak(event: BlockBreakEvent) {
        val location = event.block.location
        val tileEntity = getTileEntityAt(location)
        if (tileEntity != null) {
            event.isCancelled = true
            destroyAndDropTileEntity(tileEntity, event.player.gameMode == GameMode.SURVIVAL)
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handleInteract(event: PlayerInteractEvent) {
        val action = event.action
        val player = event.player
        if (action == Action.RIGHT_CLICK_BLOCK && !event.player.isSneaking) {
            val block = event.clickedBlock!!
            val tileEntity = getTileEntityAt(block.location)
            if (tileEntity != null && ProtectionUtils.canUse(player, block.location)) tileEntity.handleRightClick(event)
        } else if (action == Action.LEFT_CLICK_BLOCK) {
            val block = event.clickedBlock!!
            if (block.type == Material.BARRIER
                && event.player.gameMode == GameMode.SURVIVAL
                && getTileEntityAt(block.location) != null
                && ProtectionUtils.canBreak(player, block.location)) {
                
                event.isCancelled = true
                Bukkit.getPluginManager().callEvent(BlockBreakEvent(block, player))
                player.playSound(block.location, Sound.BLOCK_STONE_BREAK, 1f, 1f)
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handleInventoryCreative(event: InventoryCreativeEvent) {
        val player = event.whoClicked as Player
        val targetBlock = player.getTargetBlockExact(8)
        if (targetBlock != null && targetBlock.type == event.cursor.type) {
            val tileEntity = getTileEntityAt(targetBlock.location)
            if (tileEntity != null) {
                val novaMaterial = tileEntity.material
                event.cursor = novaMaterial.createItemStack()
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handlePistonExtend(event: BlockPistonExtendEvent) {
        if (event.blocks.any { it.location in locationCache }) event.isCancelled = true
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handlePistonRetract(event: BlockPistonRetractEvent) {
        if (event.blocks.any { it.location in locationCache }) event.isCancelled = true
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handleBlockPhysics(event: BlockPhysicsEvent) {
        val location = event.block.location
        if (location in locationCache && Material.AIR == event.block.type) {
            val tileEntity = getTileEntityAt(location)
            if (tileEntity != null)
                destroyAndDropTileEntity(tileEntity, true)
        }
    }
    
    private fun handleExplosion(blockList: MutableList<Block>) {
        val tiles = blockList.filter { it.location in locationCache }
        blockList.removeAll(tiles)
        tiles.forEach { destroyAndDropTileEntity(getTileEntityAt(it.location)!!, true) }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handleEntityExplosion(event: EntityExplodeEvent) = handleExplosion(event.blockList())
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handleBlockExplosion(event: BlockExplodeEvent) = handleExplosion(event.blockList())
    
}
