package xyz.xenondevs.nova.tileentity

import net.dzikoysk.exposed.upsert.upsert
import net.md_5.bungee.api.ChatColor
import org.bukkit.*
import org.bukkit.block.Block
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
import org.bukkit.event.world.WorldSaveEvent
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.database.asyncTransaction
import xyz.xenondevs.nova.data.database.entity.DaoTileEntity
import xyz.xenondevs.nova.data.database.table.TileEntitiesTable
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.data.serialization.persistentdata.CompoundElementDataType
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.world.armorstand.AsyncChunkPos
import xyz.xenondevs.nova.world.armorstand.pos
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

@Suppress("DEPRECATION")
val Material?.requiresLight: Boolean
    get() = this != null && !isTransparent && isOccluding

object TileEntityManager : Listener {
    
    private val tileEntityMap = HashMap<AsyncChunkPos, HashMap<Location, TileEntity>>()
    private val additionalHitboxMap = HashMap<AsyncChunkPos, HashMap<Location, TileEntity>>()
    private val locationCache = HashSet<Location>()
    private val tileEntities: Sequence<TileEntity>
        get() = tileEntityMap.asSequence().flatMap { (_, chunkMap) -> chunkMap.values }
    val tileEntityChunks: Sequence<Chunk>
        get() = tileEntityMap.keys.asSequence().map(AsyncChunkPos::chunk)
    
    fun init() {
        LOGGER.info("Initializing TileEntityManager")
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
        Bukkit.getWorlds().flatMap { it.loadedChunks.asList() }.forEach(this::handleChunkLoad)
        NOVA.disableHandlers += { Bukkit.getWorlds().flatMap { it.loadedChunks.asList() }.forEach(this::handleChunkUnload) }
        runTaskTimerSynchronized(this, 0, 1) { tileEntities.forEach(TileEntity::handleTick) }
        
        // TODO: Change this to the same behavior as in VanillaTileEntityManager
        runTaskTimerSynchronized(this, 0, 1200) {
            // In some special cases no event is called when replacing a block. So we check for air blocks every minute.
            tileEntities.associateWith(TileEntity::location).forEach { (tileEntity, location) ->
                if (Material.AIR == location.block.type)
                    destroyTileEntity(tileEntity, false)
            }
        }
    }
    
    @Synchronized
    fun placeTileEntity(
        ownerUUID: UUID,
        location: Location,
        yaw: Float,
        material: NovaMaterial,
        data: CompoundElement?,
        tileEntityUUID: UUID? = null
    ) {
        val block = location.block
        val chunk = location.chunk
        
        // create TileEntity with FakeArmorStand
        val spawnLocation = location
            .clone()
            .add(0.5, 0.0, 0.5)
            .also { it.yaw = calculateTileEntityYaw(material, yaw) }
        
        val uuid = tileEntityUUID ?: UUID.randomUUID()
        val tileEntity = TileEntity.create(
            uuid,
            spawnLocation,
            material,
            data ?: CompoundElement().apply { putElement("global", CompoundElement()) },
            ownerUUID,
        )
        
        // add to tileEntities map
        val chunkMap = tileEntityMap.getOrPut(chunk.pos) { HashMap() }
        chunkMap[location] = tileEntity
        
        // add to location cache
        locationCache += location
        
        // count for TileEntity limits
        TileEntityLimits.handleTileEntityCreate(ownerUUID, material)
        
        // call handleInitialized
        tileEntity.handleInitialized(true)
        
        // set the hitbox block (1 tick later to prevent interference with the BlockBreakEvent)
        runTaskLater(1) {
            if (tileEntity.isValid) { // check that the tile entity hasn't been destroyed already
                material.hitboxType?.run { block.type = this }
                tileEntity.handleHitboxPlaced()
            }
        }
    }
    
    private fun calculateTileEntityYaw(material: NovaMaterial, playerYaw: Float): Float =
        if (material.isDirectional) ((playerYaw + 180).mod(360f) / 90f).roundToInt() * 90f else 180f
    
    @Synchronized
    fun destroyTileEntity(tileEntity: TileEntity, dropItems: Boolean, deleteInDatabase: Boolean = true): List<ItemStack> {
        val location = tileEntity.location
        val chunkPos = location.chunkPos
        
        // remove TileEntity and ArmorStand
        tileEntityMap[chunkPos]?.remove(location)
        locationCache -= location
        tileEntity.additionalHitboxes.forEach {
            tileEntityMap[it.chunkPos]?.remove(it)
            locationCache -= it
        }
        
        if (deleteInDatabase) { // remove it from the database
            asyncTransaction {
                TileEntitiesTable.deleteWhere { TileEntitiesTable.id eq tileEntity.uuid }
            }
        }
        
        location.block.type = Material.AIR
        tileEntity.additionalHitboxes.forEach { it.block.type = Material.AIR }
        val drops = tileEntity.destroy(dropItems) // destroy tileEntity and save drops for later
        
        tileEntity.handleRemoved(unload = false)
        
        // count for TileEntity limits
        TileEntityLimits.handleTileEntityRemove(tileEntity.ownerUUID, tileEntity.material)
        
        return drops
    }
    
    @Synchronized
    fun destroyAndDropTileEntity(tileEntity: TileEntity, dropItems: Boolean) {
        val drops = destroyTileEntity(tileEntity, dropItems)
        
        // drop items a tick later to prevent interference with the cancellation of the break event
        runTaskLater(1) { tileEntity.location.dropItems(drops) }
    }
    
    @Synchronized
    fun getTileEntityAt(location: Location, additionalHitboxes: Boolean = true): TileEntity? {
        val chunkPos = location.chunkPos
        return tileEntityMap[chunkPos]?.get(location)
            ?: if (additionalHitboxes) additionalHitboxMap[chunkPos]?.get(location) else null
    }
    
    @Synchronized
    fun getTileEntitiesInChunk(chunkPos: AsyncChunkPos) = tileEntityMap[chunkPos]?.values?.toList() ?: emptyList()
    
    @Synchronized
    fun addTileEntityHitboxLocations(tileEntity: TileEntity, locations: List<Location>) {
        locations.forEach {
            val chunkMap = additionalHitboxMap.getOrPut(it.chunkPos) { HashMap() }
            chunkMap[it] = tileEntity
            
            locationCache += it
        }
    }
    
    private fun handleChunkLoad(chunk: Chunk) {
        asyncTransaction {
            DaoTileEntity.find { (TileEntitiesTable.world eq chunk.world.uid) and (TileEntitiesTable.chunkX eq chunk.x) and (TileEntitiesTable.chunkZ eq chunk.z) }
                .forEach { tile ->
                    tile.inventories.forEach { inventory ->
                        TileInventoryManager.loadInventory(tile.id.value, inventory.id.value, inventory.data)
                    }
                    
                    // create the tile entity in the main thread
                    runTaskSynchronized(TileEntityManager) {
                        val location = tile.location
                        val tileEntity = TileEntity.create(tile, location)
                        
                        val chunkMap = tileEntityMap.getOrPut(chunk.pos) { HashMap() }
                        chunkMap[location] = tileEntity
                        
                        locationCache += location
                        
                        tileEntity.handleInitialized(false)
                    }
                }
        }
    }
    
    @Synchronized
    private fun handleChunkUnload(chunk: Chunk) {
        saveChunk(chunk)
        
        val chunkPos = chunk.pos
        val tileEntities = tileEntityMap[chunkPos]
        tileEntityMap -= chunkPos
        locationCache.removeAll { it.chunk == chunk }
        tileEntities?.forEach { (_, tileEntity) -> tileEntity.handleRemoved(unload = true) }
    }
    
    @Synchronized
    fun saveChunk(chunk: Chunk) {
        val tileEntities = tileEntityMap[chunk.pos]?.values ?: return
        
        transaction {
            tileEntities.forEach { tileEntity ->
                tileEntity.saveData()
                
                TileEntitiesTable.upsert(
                    conflictColumn = TileEntitiesTable.id,
                    
                    insertBody = {
                        val location = tileEntity.location
                        
                        it[id] = tileEntity.uuid
                        it[world] = tileEntity.location.world!!.uid
                        it[owner] = tileEntity.ownerUUID
                        it[chunkX] = chunk.x
                        it[chunkZ] = chunk.z
                        it[x] = location.blockX
                        it[y] = location.blockY
                        it[z] = location.blockZ
                        it[yaw] = tileEntity.armorStand.location.yaw
                        it[type] = tileEntity.material
                        it[data] = tileEntity.data
                    },
                    
                    updateBody = {
                        it[data] = tileEntity.data
                    }
                )
            }
        }
    }
    
    @EventHandler
    fun handleWorldSave(event: WorldSaveEvent) {
        runAsyncTask { event.world.loadedChunks.forEach { saveChunk(it) } }
    }
    
    @EventHandler
    fun handleChunkLoad(event: ChunkLoadEvent) {
        handleChunkLoad(event.chunk)
    }
    
    @EventHandler
    fun handleChunkUnload(event: ChunkUnloadEvent) {
        handleChunkUnload(event.chunk)
    }
    
    // TODO: clean up
    @Synchronized
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handlePlace(event: BlockPlaceEvent) {
        val location = event.blockPlaced.location
        
        if (getTileEntityAt(location) == null) {
            val player = event.player
            val placedItem = event.itemInHand
            val material = placedItem.novaMaterial
            
            if (material != null) {
                event.isCancelled = true
                
                if (material.isBlock) {
                    val playerLocation = player.location
                    
                    if (material.placeCheck?.invoke(
                            player,
                            location.apply { yaw = calculateTileEntityYaw(material, playerLocation.yaw) }
                        ) != false
                    ) {
                        val uuid = player.uniqueId
                        val result = TileEntityLimits.canPlaceTileEntity(uuid, location.world!!, material)
                        if (result == PlaceResult.ALLOW) {
                            placeTileEntity(
                                uuid,
                                event.block.location,
                                playerLocation.yaw,
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
        } else event.isCancelled = true
    }
    
    @Synchronized
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun handleBreak(event: BlockBreakEvent) {
        val location = event.block.location
        val tileEntity = getTileEntityAt(location)
        if (tileEntity != null) {
            event.isCancelled = true
            destroyAndDropTileEntity(tileEntity, event.player.gameMode == GameMode.SURVIVAL)
        }
    }
    
    @Synchronized
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handleInteract(event: PlayerInteractEvent) {
        val action = event.action
        val player = event.player
        if (action == Action.RIGHT_CLICK_BLOCK && !event.player.isSneaking) {
            val block = event.clickedBlock!!
            val tileEntity = getTileEntityAt(block.location)
            if (tileEntity != null && ProtectionManager.canUse(player, block.location)) tileEntity.handleRightClick(event)
        } else if (action == Action.LEFT_CLICK_BLOCK) {
            val block = event.clickedBlock!!
            if ((block.type == Material.BARRIER || block.type == Material.CHAIN)
                && event.player.gameMode == GameMode.SURVIVAL
                && getTileEntityAt(block.location) != null
                && ProtectionManager.canBreak(player, block.location)) {
                
                event.isCancelled = true
                Bukkit.getPluginManager().callEvent(BlockBreakEvent(block, player))
                player.playSound(block.location, Sound.BLOCK_STONE_BREAK, 1f, 1f)
            }
        }
    }
    
    @Synchronized
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
    
    @Synchronized
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handlePistonExtend(event: BlockPistonExtendEvent) {
        if (event.blocks.any { it.location in locationCache }) event.isCancelled = true
    }
    
    @Synchronized
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handlePistonRetract(event: BlockPistonRetractEvent) {
        if (event.blocks.any { it.location in locationCache }) event.isCancelled = true
    }
    
    @Synchronized
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun handleBlockPhysics(event: BlockPhysicsEvent) {
        val location = event.block.location
        if (location in locationCache && Material.AIR == event.block.type) {
            val tileEntity = getTileEntityAt(location)
            if (tileEntity != null)
                destroyAndDropTileEntity(tileEntity, true)
        }
    }
    
    @Synchronized
    private fun handleExplosion(blockList: MutableList<Block>) {
        val tiles = blockList.filter { it.location in locationCache }
        blockList.removeAll(tiles)
        tiles.forEach { destroyAndDropTileEntity(getTileEntityAt(it.location)!!, true) }
    }
    
    @Synchronized
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handleEntityExplosion(event: EntityExplodeEvent) = handleExplosion(event.blockList())
    
    @Synchronized
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handleBlockExplosion(event: BlockExplodeEvent) = handleExplosion(event.blockList())
    
}
