package xyz.xenondevs.nova.tileentity

import net.dzikoysk.exposed.upsert.upsert
import net.md_5.bungee.api.ChatColor
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.inventory.InventoryCreativeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.event.world.WorldSaveEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.database.DatabaseManager
import xyz.xenondevs.nova.data.database.asyncTransaction
import xyz.xenondevs.nova.data.database.entity.DaoTileEntity
import xyz.xenondevs.nova.data.database.table.TileEntitiesTable
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.data.serialization.persistentdata.CompoundElementDataType
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.material.CoreItems
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.material.TileEntityNovaMaterial
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.util.concurrent.CombinedBooleanFuture
import xyz.xenondevs.nova.util.concurrent.runIfTrue
import xyz.xenondevs.nova.util.concurrent.runIfTrueSynchronized
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.armorstand.FakeArmorStandManager
import xyz.xenondevs.nova.world.pos
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt
import xyz.xenondevs.nova.api.tileentity.TileEntityManager as ITileEntityManager

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

object TileEntityManager : Initializable(), ITileEntityManager, Listener {
    
    private val tileEntityMap = HashMap<ChunkPos, HashMap<Location, TileEntity>>()
    private val additionalHitboxMap = HashMap<ChunkPos, HashMap<Location, TileEntity>>()
    private val locationCache = HashSet<Location>()
    val tileEntities: Sequence<TileEntity>
        get() = tileEntityMap.asSequence().flatMap { (_, chunkMap) -> chunkMap.values }
    val tileEntityChunks: Sequence<ChunkPos>
        get() = tileEntityMap.keys.asSequence()
    
    private val chunkProcessors = HashMap<ChunkPos, ChunkProcessor>()
    
    override val inMainThread = true
    override val dependsOn = setOf(AddonsInitializer, Resources, DatabaseManager, FakeArmorStandManager, NovaConfig)
    
    override fun init() {
        LOGGER.info("Initializing TileEntityManager")
        
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
        Bukkit.getWorlds().flatMap { it.loadedChunks.asList() }.forEach { handleChunkLoad(it.pos) }
        NOVA.disableHandlers += { Bukkit.getWorlds().flatMap { it.loadedChunks.asList() }.forEach { handleChunkUnload(it.pos) } }
        
        runTaskTimerSynchronized(this, 0, 1) { tileEntities.forEach(TileEntity::handleTick) }
        runAsyncTaskTimerSynchronized(this, 0, 1) { tileEntities.forEach(TileEntity::handleAsyncTick) }
        
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
        material: TileEntityNovaMaterial,
        data: CompoundElement?,
        tileEntityUUID: UUID? = null
    ) {
        val block = location.block
        val blockLocation = block.location
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
        chunkMap[blockLocation] = tileEntity
        
        // add to location cache
        locationCache += blockLocation
        
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
    
    private fun calculateTileEntityYaw(material: TileEntityNovaMaterial, playerYaw: Float): Float =
        if (material.isDirectional) ((playerYaw + 180).mod(360f) / 90f).roundToInt() * 90f else 180f
    
    @Synchronized
    fun removeTileEntity(tileEntity: TileEntity) {
        val location = tileEntity.location
        val chunkPos = location.chunkPos
        
        // remove TileEntity and ArmorStand
        tileEntityMap[chunkPos]?.remove(location)
        locationCache -= location
        tileEntity.additionalHitboxes.forEach {
            additionalHitboxMap[it.chunkPos]?.remove(it)
            locationCache -= it
        }
        
        asyncTransaction {
            TileEntitiesTable.deleteWhere { TileEntitiesTable.id eq tileEntity.uuid }
        }
        
        location.block.type = Material.AIR
        tileEntity.additionalHitboxes.forEach { it.block.type = Material.AIR }
        
        tileEntity.handleRemoved(unload = false)
        
        // count for TileEntity limits
        TileEntityLimits.handleTileEntityRemove(tileEntity.ownerUUID, tileEntity.material)
    }
    
    @Synchronized
    fun destroyTileEntity(tileEntity: TileEntity, dropItems: Boolean): List<ItemStack> {
        removeTileEntity(tileEntity)
        return tileEntity.getDrops(dropItems)
    }
    
    @Synchronized
    fun destroyAndDropTileEntity(tileEntity: TileEntity, dropItems: Boolean) {
        val drops = destroyTileEntity(tileEntity, dropItems)
        
        // drop items a tick later to prevent interference with the cancellation of the break event
        runTaskLater(1) { tileEntity.location.dropItems(drops) }
    }
    
    @Synchronized
    override fun getTileEntityAt(location: Location): TileEntity? {
        return getTileEntityAt(location, true)
    }
    
    @Synchronized
    fun getTileEntityAt(location: Location, additionalHitboxes: Boolean): TileEntity? {
        val chunkPos = location.chunkPos
        val blockLocation = location.blockLocation
        return tileEntityMap[chunkPos]?.get(blockLocation)
            ?: if (additionalHitboxes) additionalHitboxMap[chunkPos]?.get(blockLocation) else null
    }
    
    @Synchronized
    fun getTileEntitiesInChunk(chunkPos: ChunkPos) = tileEntityMap[chunkPos]?.values?.toList() ?: emptyList()
    
    @Synchronized
    fun addTileEntityHitboxLocations(tileEntity: TileEntity, locations: List<Location>) {
        locations.forEach {
            val chunkMap = additionalHitboxMap.getOrPut(it.chunkPos) { HashMap() }
            chunkMap[it] = tileEntity
            
            locationCache += it
        }
    }
    
    private fun handleChunkLoad(chunkPos: ChunkPos) {
        setChunkProcessorGoal(chunkPos, ChunkProcessor.Goal.LOAD)
    }
    
    @Synchronized
    private fun handleChunkUnload(chunkPos: ChunkPos) {
        setChunkProcessorGoal(chunkPos, ChunkProcessor.Goal.UNLOAD)
    }
    
    @Synchronized
    fun saveChunk(chunk: ChunkPos) {
        if (chunk in tileEntityMap)
            saveChunk(HashSet(tileEntityMap[chunk]!!.values))
    }
    
    private fun saveChunk(tileEntities: Iterable<TileEntity>) {
        val statement: (Transaction.() -> Unit) = {
            tileEntities.forEach { tileEntity ->
                tileEntity.saveData()
                
                TileEntitiesTable.upsert(
                    conflictColumn = TileEntitiesTable.id,
                    
                    insertBody = {
                        val location = tileEntity.location
                        
                        it[id] = tileEntity.uuid
                        it[world] = tileEntity.location.world!!.uid
                        it[owner] = tileEntity.ownerUUID
                        it[chunkX] = tileEntity.chunkPos.x
                        it[chunkZ] = tileEntity.chunkPos.z
                        it[x] = location.blockX
                        it[y] = location.blockY
                        it[z] = location.blockZ
                        it[yaw] = tileEntity.armorStand.location.yaw
                        it[type] = tileEntity.material.id
                        it[data] = tileEntity.data
                    },
                    
                    updateBody = {
                        it[data] = tileEntity.data
                    }
                )
            }
        }
        
        if (NOVA.isEnabled) asyncTransaction(statement)
        else transaction(statement = statement)
    }
    
    @EventHandler
    private fun handleWorldSave(event: WorldSaveEvent) {
        runAsyncTask { event.world.loadedChunks.forEach { saveChunk(it.pos) } }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    private fun handleChunkLoad(event: ChunkLoadEvent) {
        handleChunkLoad(event.chunk.pos)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    private fun handleChunkUnload(event: ChunkUnloadEvent) {
        handleChunkUnload(event.chunk.pos)
    }
    
    @Synchronized
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun handleBreak(event: BlockBreakEvent) {
        val location = event.block.location
        val tileEntity = getTileEntityAt(location)
        if (tileEntity != null) {
            event.isCancelled = true
            destroyAndDropTileEntity(tileEntity, event.player.gameMode == GameMode.SURVIVAL)
        }
    }
    
    private fun handleTileEntityInteract(event: PlayerInteractEvent, tileEntity: TileEntity) {
        if (event.hand == EquipmentSlot.HAND) {
            val player = event.player
            val block = event.clickedBlock!!
            if (!player.isSneaking) {
                event.isCancelled = true
                ProtectionManager.canUseBlock(player, event.item, block.location).runIfTrue {
                    tileEntity.handleRightClick(event)
                }
            } else if (event.handItems.any { it.novaMaterial == CoreItems.WRENCH }) {
                ProtectionManager.canBreak(player, event.item, block.location).runIfTrueSynchronized(TileEntityManager) {
                    destroyAndDropTileEntity(tileEntity, player.gameMode == GameMode.SURVIVAL)
                }
            }
        } else event.isCancelled = true
    }
    
    private fun handleTileEntityWrenchShift(event: PlayerInteractEvent, tileEntity: TileEntity) {
        ProtectionManager.canBreak(event.player, event.item, event.clickedBlock!!.location).runIfTrueSynchronized(TileEntityManager) {
            destroyAndDropTileEntity(tileEntity, event.player.gameMode == GameMode.SURVIVAL)
        }
    }
    
    private fun handleTileEntityPlace(event: PlayerInteractEvent, material: TileEntityNovaMaterial) {
        event.isCancelled = true
        
        val player = event.player
        val handItem = event.item!!
        val playerLocation = player.location
        
        val placePos = event.clickedBlock!!.location.advance(event.blockFace)
        val block = placePos.block
        
        val placeFuture = if (material.placeCheck != null) {
            CombinedBooleanFuture(
                ProtectionManager.canPlace(player, handItem, placePos),
                material.placeCheck.invoke(player, handItem, placePos.apply { yaw = calculateTileEntityYaw(material, playerLocation.yaw) })
            )
        } else ProtectionManager.canPlace(player, handItem, placePos)
        
        placeFuture.runIfTrueSynchronized(TileEntityManager) {
            if (placePos.block.type.isAir) {
                val uuid = player.uniqueId
                val result = TileEntityLimits.canPlaceTileEntity(uuid, placePos.world!!, material)
                if (result == PlaceResult.ALLOW) {
                    placeTileEntity(
                        uuid,
                        placePos,
                        playerLocation.yaw,
                        material,
                        handItem.getTileEntityData()?.let { CompoundElement().apply { putElement("global", it) } }
                    )
                    
                    if (player.gameMode == GameMode.SURVIVAL) handItem.amount--
                    
                    player.swingMainHand()
                    player.playSound(block.location, material.hitboxType!!.soundGroup.placeSound, 1f, 1f)
                } else {
                    player.spigot().sendMessage(
                        localized(ChatColor.RED, "nova.tile_entity_limits.${result.name.lowercase()}")
                    )
                }
            }
        }
    }
    
    private fun handleNormalBlockPlace(event: PlayerInteractEvent) {
        event.isCancelled = true
        
        val block = event.clickedBlock!!
        val handItem = event.item!!
        val player = event.player
        
        val replaceLocation = block.location.advance(event.blockFace)
        val replaceBlock = replaceLocation.block
        
        if (replaceBlock.type.isReplaceable() && getTileEntityAt(replaceLocation) == null) {
            val previousType = replaceBlock.type
            replaceBlock.type = handItem.type
            val placeEvent = BlockPlaceEvent(replaceBlock, replaceBlock.state, block, handItem, player, true, event.hand!!)
            Bukkit.getPluginManager().callEvent(placeEvent)
            if (placeEvent.isCancelled) {
                replaceBlock.type = previousType
            } else if (player.gameMode != GameMode.CREATIVE) {
                player.inventory.setItem(event.hand!!, handItem.apply { amount -= 1 })
            }
        }
    }
    
    private fun handlePossibleTileEntityDestroy(event: PlayerInteractEvent, tileEntity: TileEntity) {
        val block = event.clickedBlock!!
        val player = event.player
        
        if ((block.type == Material.BARRIER || block.type == Material.CHAIN)
            && event.player.gameMode == GameMode.SURVIVAL
            && getTileEntityAt(block.location) != null
        ) {
            event.isCancelled = true
            
            ProtectionManager.canBreak(player, event.item, block.location).runIfTrueSynchronized(TileEntityManager) {
                // The tile entity might have been destroyed by now
                if (getTileEntityAt(block.location) != tileEntity) {
                    Bukkit.getPluginManager().callEvent(BlockBreakEvent(block, player))
                    player.playSound(block.location, Sound.BLOCK_STONE_BREAK, 1f, 1f)
                }
            }
        }
    }
    
    @Synchronized
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    private fun handleInteract(event: PlayerInteractEvent) {
        val action = event.action
        val player = event.player
        if (action == Action.RIGHT_CLICK_BLOCK) {
            val handItem = event.item
            val block = event.clickedBlock!!
            val tileEntity = getTileEntityAt(block.location)
            
            if (tileEntity != null && !player.isSneaking && tileEntity.material.isInteractable) {
                handleTileEntityInteract(event, tileEntity)
            } else {
                val handNovaMaterial = handItem?.novaMaterial
                if (tileEntity != null && player.isSneaking && handNovaMaterial == CoreItems.WRENCH) {
                    handleTileEntityWrenchShift(event, tileEntity)
                } else if (!block.type.isActuallyInteractable() || player.isSneaking) {
                    if (handNovaMaterial is TileEntityNovaMaterial) {
                        handleTileEntityPlace(event, handNovaMaterial)
                    } else if (tileEntity != null && block.type.isReplaceable() && handNovaMaterial == null && handItem?.type?.isBlock == true) {
                        handleNormalBlockPlace(event)
                    }
                }
            }
        } else if (action == Action.LEFT_CLICK_BLOCK) {
            val block = event.clickedBlock!!
            val tileEntity = getTileEntityAt(block.location)
            if (tileEntity != null) handlePossibleTileEntityDestroy(event, tileEntity)
        }
    }
    
    @Synchronized
    @EventHandler
    private fun handleBlockPlace(event: BlockPlaceEvent) {
        if (getTileEntityAt(event.block.location) != null)
            event.isCancelled = true
    }
    
    @Synchronized
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleInventoryCreative(event: InventoryCreativeEvent) {
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
    private fun handlePistonExtend(event: BlockPistonExtendEvent) {
        if (event.blocks.any { it.location in locationCache }) event.isCancelled = true
    }
    
    @Synchronized
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handlePistonRetract(event: BlockPistonRetractEvent) {
        if (event.blocks.any { it.location in locationCache }) event.isCancelled = true
    }
    
    @Synchronized
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun handleBlockPhysics(event: BlockPhysicsEvent) {
        val location = event.block.location
        if (location in locationCache && Material.AIR == event.block.type) {
            val tileEntity = getTileEntityAt(location)
            if (tileEntity != null && tileEntity.hasHitboxBeenPlaced)
                destroyAndDropTileEntity(tileEntity, true)
        }
    }
    
    @Synchronized
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun handleEntityChangeBlock(event: EntityChangeBlockEvent) {
        val type = event.entityType
        if ((type == EntityType.SILVERFISH || type == EntityType.ENDERMAN) && event.block.location in locationCache)
            event.isCancelled = true
    }
    
    @Synchronized
    private fun handleExplosion(blockList: MutableList<Block>) {
        val tiles = blockList.filter { it.location in locationCache }
        blockList.removeAll(tiles)
        tiles.forEach { destroyAndDropTileEntity(getTileEntityAt(it.location)!!, true) }
    }
    
    @Synchronized
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleEntityExplosion(event: EntityExplodeEvent) = handleExplosion(event.blockList())
    
    @Synchronized
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleBlockExplosion(event: BlockExplodeEvent) = handleExplosion(event.blockList())
    
    private fun setChunkProcessorGoal(chunkPos: ChunkPos, goal: ChunkProcessor.Goal) {
        synchronized(chunkProcessors) {
            val currentProcessor = chunkProcessors[chunkPos]
            if (currentProcessor != null) {
                if (NOVA.isEnabled) {
                    currentProcessor.goal = goal
                } else {
                    // ignore current processor as that runs in an async task
                    ChunkProcessor(chunkPos, goal)
                }
            } else chunkProcessors[chunkPos] = ChunkProcessor(chunkPos, goal)
        }
    }
    
    private class ChunkProcessor(private val chunkPos: ChunkPos, @Volatile var goal: Goal) {
        
        enum class Goal {
            LOAD,
            UNLOAD
        }
        
        init {
            val processTaskAsync = NOVA.isEnabled
            
            if (!processTaskAsync && goal == Goal.LOAD)
                throw IllegalStateException("Loading Chunks is not allowed while the plugin is disabled")
            
            val task = {
                do {
                    val currentGoal = goal
                    
                    // Nova might have been disabled while this processor was running.
                    // Since we can't switch back to the main thread, and we're currently running async, we cannot do anything.
                    // If the chunk is currently loaded, it will get unloaded by another ChunkProcessor.
                    if (processTaskAsync && !NOVA.isEnabled) break
                    
                    val done = AtomicBoolean()
                    if (currentGoal == Goal.LOAD) {
                        // chunk loading is always done async as it retrieves data from the database
                        loadChunk(done)
                    } else {
                        // chunk unloading is always done in the main thread
                        if (processTaskAsync) runTask { unloadChunk(done) }
                        unloadChunk(done)
                    }
                    
                    // wait until the process is done or nova is disabled
                    while (!done.get() && NOVA.isEnabled) Thread.sleep(1)
                    
                } while (currentGoal != goal) // repeat if the goal has changed
                
                // this processor is no longer required
                synchronized(chunkProcessors) { chunkProcessors -= chunkPos }
            }
            
            if (processTaskAsync) runAsyncTask(task)
            else task()
        }
        
        private fun unloadChunk(done: AtomicBoolean) {
            synchronized(TileEntityManager) {
                if (chunkPos in tileEntityMap) {
                    val tileEntities = tileEntityMap[chunkPos]!!
                    val tileEntityValues = HashSet(tileEntities.values)
                    
                    tileEntityMap -= chunkPos
                    additionalHitboxMap -= chunkPos
                    locationCache.removeAll { it.chunkPos == chunkPos }
                    tileEntityValues.forEach { it.handleRemoved(unload = true) }
                    
                    saveChunk(tileEntityValues)
                }
                
                done.set(true)
            }
        }
        
        private fun loadChunk(done: AtomicBoolean) {
            if (chunkPos.isLoaded()) {
                transaction {
                    val tileEntities = DaoTileEntity.find { (TileEntitiesTable.world eq chunkPos.worldUUID) and (TileEntitiesTable.chunkX eq chunkPos.x) and (TileEntitiesTable.chunkZ eq chunkPos.z) }.toList()
                    
                    if (!NOVA.isEnabled) return@transaction
                    
                    // create the tile entities in the main thread
                    runTaskSynchronized(TileEntityManager) {
                        val chunkMap = tileEntityMap.getOrPut(chunkPos) { HashMap() }
                        
                        tileEntities.forEach { tile ->
                            if (NovaMaterialRegistry.getOrNull(tile.type) == null) {
                                LOGGER.severe("Could not load tile entity at ${tile.location}: Invalid id ${tile.type}")
                                return@forEach
                            }
                            
                            try {
                                val location = tile.location
                                val tileEntity = TileEntity.create(tile, location)
                                
                                chunkMap[location] = tileEntity
                                locationCache += location
                                
                                tileEntity.handleInitialized(false)
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                        
                        NetworkManager.queueChunkLoad(chunkPos)
                        
                        done.set(true)
                    }
                }
            } else done.set(true)
        }
        
    }
    
}
