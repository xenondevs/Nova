package xyz.xenondevs.nova.tileentity

import com.google.gson.JsonObject
import net.md_5.bungee.api.ChatColor
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
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.database.asyncTransaction
import xyz.xenondevs.nova.database.table.TileEntitiesTable
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.serialization.cbf.element.CompoundDeserializer
import xyz.xenondevs.nova.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.serialization.persistentdata.CompoundElementDataType
import xyz.xenondevs.nova.serialization.persistentdata.JsonElementDataType
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.util.protection.ProtectionUtils
import java.util.*
import kotlin.math.roundToInt

val TILE_ENTITY_KEY = NamespacedKey(NOVA, "tileEntity")

fun ItemStack.setTileEntityData(data: CompoundElement) {
    if (hasItemMeta()) {
        val itemMeta = this.itemMeta!!
        val dataContainer = itemMeta.persistentDataContainer
        dataContainer.set(TILE_ENTITY_KEY, CompoundElementDataType, data)
        this.itemMeta = itemMeta
    }
}

fun ItemStack.getTileEntityData(): CompoundElement? {
    if (hasItemMeta()) {
        val dataContainer = itemMeta!!.persistentDataContainer
        if (dataContainer.has(TILE_ENTITY_KEY, CompoundElementDataType)) {
            return dataContainer.get(TILE_ENTITY_KEY, CompoundElementDataType)
        }
    }
    
    return null
}

fun ArmorStand.setTileEntityData(data: JsonObject) =
    persistentDataContainer.set(TILE_ENTITY_KEY, JsonElementDataType, data)

fun ArmorStand.getTileEntityData() =
    persistentDataContainer.get(TILE_ENTITY_KEY, JsonElementDataType)?.let { it as JsonObject }

fun ArmorStand.hasTileEntityData(): Boolean =
    persistentDataContainer.has(TILE_ENTITY_KEY, JsonElementDataType)

@Suppress("DEPRECATION")
val Material?.requiresLight: Boolean
    get() = this != null && !isTransparent

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
        data: CompoundElement?
    ) {
        val block = location.block
        val chunk = location.chunk
        
        // create TileEntity with FakeArmorStand
        val spawnLocation = location
            .clone()
            .add(0.5, 0.0, 0.5)
            .also { it.yaw = if (material.isDirectional) ((yaw + 180).mod(360f) / 90f).roundToInt() * 90f else 180f }
        
        val uuid = UUID.randomUUID()
        val tileEntity = TileEntity.create(
            uuid,
            spawnLocation,
            data ?: CompoundElement().apply { putElement("global", CompoundElement()) },
            ownerUUID,
            material,
        )
        
        // add to tileEntities map
        val chunkMap = tileEntityMap[chunk] ?: HashMap<Location, TileEntity>().also { tileEntityMap[chunk] = it }
        chunkMap[location] = tileEntity
        
        // add to location cache
        locationCache += location
        
        // count for TileEntity limits
        TileEntityLimits.handleTileEntityCreate(ownerUUID, material)
        
        runTaskLater(1) {
            // set the hitbox block (1 tick later to prevent interference with the BlockBreakEvent)
            material.hitbox?.run { block.type = this }
            // handle finished initializing
            tileEntity.handleInitialized(true)
            // save the tile entity to the database
            asyncTransaction {
                tileEntity.saveData()
                TileEntitiesTable.insert {
                    it[this.uuid] = uuid
                    it[world] = location.world!!.uid
                    it[chunkX] = chunk.x
                    it[chunkZ] = chunk.z
                    it[x] = location.blockX
                    it[y] = location.blockY
                    it[z] = location.blockZ
                    it[this.yaw] = spawnLocation.yaw
                    it[this.data] = ExposedBlob(tileEntity.getData())
                }
            }
        }
    }
    
    fun destroyTileEntity(tileEntity: TileEntity, dropItems: Boolean): List<ItemStack> {
        val location = tileEntity.location
        val chunk = location.chunk
        
        // remove TileEntity and ArmorStand
        tileEntityMap[chunk]?.remove(location)
        locationCache -= location
        tileEntity.additionalHitboxes.forEach {
            tileEntityMap[it.chunk]?.remove(it)
            locationCache -= it
        }
        tileEntity.armorStand.remove()
        
        // remove it from the database
        asyncTransaction {
            TileEntitiesTable.deleteWhere { TileEntitiesTable.uuid eq tileEntity.uuid }
        }
        
        location.block.type = Material.AIR
        tileEntity.additionalHitboxes.forEach { it.block.type = Material.AIR }
        val drops = tileEntity.destroy(dropItems) // destroy tileEntity and save drops for later
        
        tileEntity.handleRemoved(unload = false)
        
        // count for TileEntity limits
        TileEntityLimits.handleTileEntityRemove(tileEntity.ownerUUID, tileEntity.material)
        
        return drops
    }
    
    fun destroyAndDropTileEntity(tileEntity: TileEntity, dropItems: Boolean) {
        val drops = destroyTileEntity(tileEntity, dropItems)
        
        // drop items a tick later to prevent interference with the cancellation of the break event
        runTaskLater(1) { tileEntity.location.dropItems(drops) }
    }
    
    fun getTileEntityAt(location: Location) = tileEntityMap[location.chunk]?.get(location)
    
    fun getTileEntitiesInChunk(chunk: Chunk) = tileEntityMap[chunk]?.values?.toList() ?: emptyList()
    
    fun addTileEntityLocations(tileEntity: TileEntity, locations: List<Location>) {
        locations.forEach {
            val chunkMap = tileEntityMap.getOrPut(it.chunk) { HashMap() }
            chunkMap[it] = tileEntity
            
            locationCache += it
        }
    }
    
    private fun handleChunkLoad(chunk: Chunk) {
        asyncTransaction {
            TileEntitiesTable
                .select { (TileEntitiesTable.world eq chunk.world.uid) and (TileEntitiesTable.chunkX eq chunk.x) and (TileEntitiesTable.chunkZ eq chunk.z) }
                .forEach {
                    val uuid = it[TileEntitiesTable.uuid]
                    val data = CompoundDeserializer.read(it[TileEntitiesTable.data].bytes)
                    
                    val location = Location(
                        Bukkit.getWorld(it[TileEntitiesTable.world]),
                        it[TileEntitiesTable.x].toDouble(),
                        it[TileEntitiesTable.y].toDouble(),
                        it[TileEntitiesTable.z].toDouble(),
                    )
                    
                    // create the tile entity in the main thread
                    runTask {
                        val tileEntity = TileEntity.create(
                            uuid,
                            location.clone().apply { center(); yaw = it[TileEntitiesTable.yaw] },
                            data
                        )
                        
                        val chunkMap = tileEntityMap.getOrPut(chunk) { HashMap() }
                        chunkMap[location] = tileEntity
                        
                        locationCache += location
                        
                        tileEntity.handleInitialized(false)
                    }
                }
        }
    }
    
    private fun handleChunkUnload(chunk: Chunk) {
        val tileEntities = tileEntityMap[chunk]
        tileEntityMap -= chunk
        locationCache.removeAll { it.chunk == chunk }
        tileEntities?.forEach { (_, tileEntity) -> tileEntity.handleRemoved(unload = true) }
    }
    
    @EventHandler
    fun handleChunkLoad(event: ChunkLoadEvent) {
        handleChunkLoad(event.chunk)
    }
    
    @EventHandler
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
                if (getTileEntityAt(location) == null && material.canPlace?.invoke(player, location) != false) {
                    val uuid = player.uniqueId
                    val result = TileEntityLimits.canPlaceTileEntity(uuid, location.world!!, material)
                    if (result == PlaceResult.ALLOW) {
                        placeTileEntity(
                            uuid,
                            event.block.location,
                            player.location.yaw,
                            material,
                            placedItem.getTileEntityData()?.let { CompoundElement().apply { putElement("global", it) } }
                        )
                        
                        if (player.gameMode == GameMode.SURVIVAL) placedItem.amount--
                    } else {
                        player.spigot().sendMessage(
                            localized(ChatColor.RED, "nova.tile_entity_limits.${result.name.lowercase()}")
                        )
                    }
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
            if ((block.type == Material.BARRIER || block.type == Material.CHAIN)
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
