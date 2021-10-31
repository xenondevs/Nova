package xyz.xenondevs.nova.world.armorstand

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.*
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.util.runAsyncTask
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.armorstand.FakeArmorStandManager.DEFAULT_RENDER_DISTANCE
import xyz.xenondevs.nova.world.armorstand.FakeArmorStandManager.RENDER_DISTANCE_KEY
import java.util.concurrent.CopyOnWriteArrayList

val Chunk.pos: ChunkPos
    get() = ChunkPos(world.uid, x, z)

var Player.armorStandRenderDistance: Int
    get() = persistentDataContainer
        .get(RENDER_DISTANCE_KEY, PersistentDataType.INTEGER)
        ?: DEFAULT_RENDER_DISTANCE
    set(value) =
        persistentDataContainer.set(RENDER_DISTANCE_KEY, PersistentDataType.INTEGER, value)


object FakeArmorStandManager : Listener {
    
    val RENDER_DISTANCE_KEY = NamespacedKey(NOVA, "armor_stand_render_distance")
    val DEFAULT_RENDER_DISTANCE = DEFAULT_CONFIG.getInt("armor_stand_render_distance.default")!!
    val MIN_RENDER_DISTANCE = DEFAULT_CONFIG.getInt("armor_stand_render_distance.min")!!
    val MAX_RENDER_DISTANCE = DEFAULT_CONFIG.getInt("armor_stand_render_distance.max")!!
    
    private val visibleChunks = HashMap<Player, Set<ChunkPos>>()
    private val chunkViewers = HashMap<ChunkPos, CopyOnWriteArrayList<Player>>()
    private val chunkArmorStands = HashMap<ChunkPos, MutableList<FakeArmorStand>>()
    
    fun init() {
        LOGGER.info("Initializing FakeArmorStandManager")
        Bukkit.getPluginManager().registerEvents(this, NOVA)
        
        Bukkit.getOnlinePlayers().forEach { player ->
            handleChunksChange(player, player.location.chunk)
        }
        
        NOVA.disableHandlers += {
            synchronized(FakeArmorStandManager) {
                chunkArmorStands.forEach { (chunk, armorStands) ->
                    val viewers = chunkViewers[chunk] ?: return@forEach
                    armorStands.forEach { armorStand -> viewers.forEach { viewer -> armorStand.despawn(viewer) } }
                }
            }
        }
    }
    
    @Synchronized
    fun getViewersOf(chunk: ChunkPos): List<Player> {
        return chunkViewers[chunk] ?: emptyList()
    }
    
    @Synchronized
    fun addArmorStand(chunk: ChunkPos, armorStand: FakeArmorStand) {
        val armorStands = chunkArmorStands.getOrPut(chunk) { mutableListOf() }
        armorStands.add(armorStand)
        
        val viewers = chunkViewers.getOrPut(chunk) { CopyOnWriteArrayList() }
        viewers.forEach { armorStand.spawn(it) }
    }
    
    @Synchronized
    fun removeArmorStand(chunk: ChunkPos, armorStand: FakeArmorStand) {
        chunkArmorStands[chunk]!!.remove(armorStand)
        chunkViewers[chunk]!!.forEach { armorStand.despawn(it) }
    }
    
    @Synchronized
    fun getChunkViewers(chunk: ChunkPos): List<Player> {
        return chunkViewers[chunk] ?: emptyList()
    }
    
    @Synchronized
    fun changeArmorStandChunk(armorStand: FakeArmorStand, previousChunk: ChunkPos, newChunk: ChunkPos) {
        // move the armor stand to the new chunk key
        chunkArmorStands[previousChunk]!!.remove(armorStand)
        chunkArmorStands.getOrPut(newChunk) { mutableListOf() }.add(armorStand)
        
        // find all players that saw the old chunk but don't see the new one and despawn it for them
        val newChunkViewers = chunkViewers.getOrPut(newChunk) { CopyOnWriteArrayList() }
        chunkViewers[previousChunk]!!.asSequence()
            .filterNot { newChunkViewers.contains(it) }
            .forEach { armorStand.despawn(it) }
        
        // find all players that didn't see the old chunk but should see the armor stand now and spawn it for them
        newChunkViewers.asSequence()
            .filterNot { (chunkViewers[previousChunk]?.contains(it) ?: false) }
            .forEach { armorStand.spawn(it) }
    }
    
    @Synchronized
    private fun handleChunksChange(player: Player, newChunk: Chunk) {
        val currentChunks = visibleChunks[player] ?: emptySet()
        val newChunks = newChunk.pos.getInRange(player.armorStandRenderDistance)
        
        // look for all chunks that are no longer visible
        currentChunks.asSequence()
            .filterNot { newChunks.contains(it) }
            .forEach { chunk ->
                // despawn the armor stands there
                chunkArmorStands[chunk]?.forEach { armorStand ->
                    armorStand.despawn(player)
                }
                
                // copy the chunkViewerList and remove the player there
                // (The list is copied to prevent other threads from causing issues by iterating over the chunkViewerList concurrently)
                chunkViewers[chunk]?.remove(player)
            }
        
        // look for all chunks weren't visible previously
        newChunks.asSequence()
            .filterNot { currentChunks.contains(it) }
            .forEach { chunk ->
                // spawn the armor stands there
                chunkArmorStands[chunk]?.forEach { armorStand ->
                    armorStand.spawn(player)
                }
                
                // copy the chunkViewerList and add the player there
                chunkViewers.getOrPut(chunk) { CopyOnWriteArrayList() }.add(player)
            }
        
        // update visible chunks map
        visibleChunks[player] = newChunks
    }
    
    @Synchronized
    private fun removeViewer(player: Player) {
        val currentChunks = visibleChunks[player]!!
        currentChunks.forEach {
            chunkViewers[it]?.remove(player)
        }
        
        visibleChunks.remove(player)
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun handleMove(event: PlayerMoveEvent) {
        val newChunk = event.to!!.chunk
        if (event.from.chunk != newChunk) {
            val player = event.player
            runAsyncTask { handleChunksChange(player, newChunk) }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun handleTeleport(event: PlayerTeleportEvent) {
        val newChunk = event.to!!.chunk
        if (event.from.chunk != newChunk) {
            val player = event.player
            runAsyncTask { handleChunksChange(player, newChunk) }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    fun handleSpawn(event: PlayerRespawnEvent) {
        val player = event.player
        val newChunk = event.respawnLocation.chunk
        if (player.location.chunk != newChunk) {
            runAsyncTask { handleChunksChange(player, newChunk) }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    fun handleJoin(event: PlayerJoinEvent) {
        val player = event.player
        val chunk = player.location.chunk
        runAsyncTask { handleChunksChange(player, chunk) }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    fun handleQuit(event: PlayerQuitEvent) {
        val player = event.player
        runAsyncTask { removeViewer(player) }
    }
    
}