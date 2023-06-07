package xyz.xenondevs.nova.world.fakeentity

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.initialize.DisableFun
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.runAsyncTask
import xyz.xenondevs.nova.util.runAsyncTaskLater
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.chunkPos
import xyz.xenondevs.nova.world.fakeentity.FakeEntityManager.DEFAULT_RENDER_DISTANCE
import xyz.xenondevs.nova.world.fakeentity.FakeEntityManager.MAX_RENDER_DISTANCE
import xyz.xenondevs.nova.world.fakeentity.FakeEntityManager.MIN_RENDER_DISTANCE
import xyz.xenondevs.nova.world.fakeentity.FakeEntityManager.RENDER_DISTANCE_KEY
import java.util.concurrent.CopyOnWriteArrayList

var Player.fakeEntityRenderDistance: Int
    get() = (persistentDataContainer.get(RENDER_DISTANCE_KEY, PersistentDataType.INTEGER) ?: DEFAULT_RENDER_DISTANCE)
        .coerceIn(MIN_RENDER_DISTANCE..MAX_RENDER_DISTANCE)
    set(value) {
        persistentDataContainer.set(RENDER_DISTANCE_KEY, PersistentDataType.INTEGER, value)
        FakeEntityManager.updateRenderDistance(this)
    }

@InternalInit(stage = InternalInitStage.POST_WORLD_ASYNC)
internal object FakeEntityManager : Listener {
    
    val RENDER_DISTANCE_KEY = NamespacedKey(NOVA, "entity_render_distance")
    val DEFAULT_RENDER_DISTANCE by configReloadable { DEFAULT_CONFIG.getInt("entity_render_distance.default") }
    val MIN_RENDER_DISTANCE by configReloadable { DEFAULT_CONFIG.getInt("entity_render_distance.min") }
    val MAX_RENDER_DISTANCE by configReloadable { DEFAULT_CONFIG.getInt("entity_render_distance.max") }
    
    private val renderDistance = HashMap<Player, Int>()
    private val visibleChunks = HashMap<Player, Set<ChunkPos>>()
    private val chunkViewers = HashMap<ChunkPos, CopyOnWriteArrayList<Player>>()
    private val chunkEntities = HashMap<ChunkPos, MutableList<FakeEntity<*>>>()
    
    @InitFun
    private fun init() {
        registerEvents()
        
        Bukkit.getOnlinePlayers().forEach { player ->
            updateRenderDistance(player)
            handleChunksChange(player, player.location.chunkPos)
        }
    }
    
    @DisableFun
    private fun disable() {
        LOGGER.info("Despawning fake entities")
        synchronized(FakeEntityManager) {
            chunkEntities.forEach { (chunk, entities) ->
                val viewers = chunkViewers[chunk] ?: return@forEach
                entities.forEach { entity -> viewers.forEach { viewer -> entity.despawn(viewer) } }
            }
        }
    }
    
    @Synchronized
    fun addEntity(chunk: ChunkPos, entity: FakeEntity<*>) {
        val entities = chunkEntities.getOrPut(chunk) { mutableListOf() }
        entities.add(entity)
        
        val viewers = chunkViewers.getOrPut(chunk) { CopyOnWriteArrayList() }
        viewers.forEach { entity.spawn(it) }
    }
    
    @Synchronized
    fun removeEntity(chunk: ChunkPos, entity: FakeEntity<*>) {
        chunkEntities[chunk]?.remove(entity)
        chunkViewers[chunk]?.forEach { entity.despawn(it) }
    }
    
    @Synchronized
    fun getChunkViewers(chunk: ChunkPos): List<Player> {
        return chunkViewers[chunk] ?: emptyList()
    }
    
    @Synchronized
    fun changeEntityChunk(entity: FakeEntity<*>, previousChunk: ChunkPos, newChunk: ChunkPos) {
        // move the armor stand to the new chunk key
        chunkEntities[previousChunk]?.remove(entity)
        chunkEntities.getOrPut(newChunk) { mutableListOf() }.add(entity)
        
        // find all players that saw the old chunk but don't see the new one and despawn it for them
        val newChunkViewers = chunkViewers.getOrPut(newChunk) { CopyOnWriteArrayList() }
        chunkViewers[previousChunk]?.asSequence()
            ?.filterNot { newChunkViewers.contains(it) }
            ?.forEach { entity.despawn(it) }
        
        // find all players that didn't see the old chunk but should see the armor stand now and spawn it for them
        newChunkViewers.asSequence()
            .filterNot { (chunkViewers[previousChunk]?.contains(it) ?: false) }
            .forEach { entity.spawn(it) }
    }
    
    @Synchronized
    private fun handleChunksChange(player: Player, newChunk: ChunkPos) {
        val currentChunks = visibleChunks[player] ?: emptySet()
        val newChunks = newChunk.getInRange(renderDistance[player] ?: 0)
        
        // look for all chunks that are no longer visible
        currentChunks.asSequence()
            .filterNot { newChunks.contains(it) }
            .forEach { chunk ->
                // despawn the armor stands there
                chunkEntities[chunk]?.forEach { it.despawn(player) }
                
                // copy the chunkViewerList and remove the player there
                // (The list is copied to prevent other threads from causing issues by iterating over the chunkViewerList concurrently)
                chunkViewers[chunk]?.remove(player)
            }
        
        // look for all chunks weren't visible previously
        newChunks.asSequence()
            .filterNot { currentChunks.contains(it) }
            .forEach { chunk ->
                // spawn the armor stands there
                chunkEntities[chunk]?.forEach { it.spawn(player) }
                
                // copy the chunkViewerList and add the player there
                chunkViewers.getOrPut(chunk) { CopyOnWriteArrayList() }.add(player)
            }
        
        // update visible chunks map
        visibleChunks[player] = newChunks
    }
    
    @Synchronized
    internal fun updateRenderDistance(player: Player) {
        renderDistance[player] = player.fakeEntityRenderDistance
    }
    
    @Synchronized
    private fun discardRenderDistance(player: Player) {
        renderDistance -= player
    }
    
    @Synchronized
    private fun removeViewer(player: Player) {
        visibleChunks[player]?.forEach {
            chunkViewers[it]?.remove(player)
        }
        
        visibleChunks.remove(player)
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun handleMove(event: PlayerMoveEvent) {
        val newChunk = event.to!!.chunkPos
        if (event.from.chunkPos != newChunk) {
            val player = event.player
            runAsyncTask { handleChunksChange(player, newChunk) }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun handleTeleport(event: PlayerTeleportEvent) {
        val newChunk = event.to!!.chunkPos
        if (event.from.chunkPos != newChunk) {
            val player = event.player
            runAsyncTaskLater(1) { handleChunksChange(player, newChunk) }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    private fun handleSpawn(event: PlayerRespawnEvent) {
        val player = event.player
        val newChunk = event.respawnLocation.chunkPos
        if (player.location.chunkPos != newChunk) {
            runAsyncTask { handleChunksChange(player, newChunk) }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    private fun handleJoin(event: PlayerJoinEvent) {
        val player = event.player
        val chunk = player.location.chunkPos
        updateRenderDistance(player)
        runAsyncTask { handleChunksChange(player, chunk) }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    private fun handleQuit(event: PlayerQuitEvent) {
        val player = event.player
        discardRenderDistance(player)
        runAsyncTask { removeViewer(player) }
    }
    
}