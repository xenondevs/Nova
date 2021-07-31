package xyz.xenondevs.nova.armorstand

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.util.runAsyncTask
import java.util.*

val Chunk.pos: AsyncChunkPos
    get() = AsyncChunkPos(world.uid, x, z)

data class AsyncChunkPos(val world: UUID, val x: Int, val z: Int) {
    
    fun getInRange(range: Int): Set<AsyncChunkPos> {
        val length = 2 * range + 1
        val chunks = HashSet<AsyncChunkPos>(length * length)
        
        for (newX in (x - range)..(x + range)) {
            for (newZ in (z - range)..(z + range)) {
                chunks.add(AsyncChunkPos(world, newX, newZ))
            }
        }
        
        return chunks
    }
    
}

object FakeArmorStandManager : Listener {
    
    private val visibleChunks = HashMap<Player, Set<AsyncChunkPos>>()
    private val chunkViewers = HashMap<AsyncChunkPos, MutableList<Player>>()
    private val chunkArmorStands = HashMap<AsyncChunkPos, MutableList<FakeArmorStand>>()
    
    fun init() {
        Bukkit.getPluginManager().registerEvents(this, NOVA)
        
        Bukkit.getOnlinePlayers().forEach { player ->
            handleChunksChange(player, player.location.chunk)
        }
    }
    
    @Synchronized
    fun addArmorStand(chunk: AsyncChunkPos, armorStand: FakeArmorStand) {
        val armorStands = chunkArmorStands.getOrPut(chunk) { mutableListOf() }
        armorStands.add(armorStand)
        
        val viewers = chunkViewers.getOrPut(chunk) { mutableListOf() }
        viewers.forEach { armorStand.spawn(it) }
    }
    
    @Synchronized
    fun removeArmorStand(chunk: AsyncChunkPos, armorStand: FakeArmorStand) {
        chunkArmorStands[chunk]!!.remove(armorStand)
        chunkViewers[chunk]!!.forEach { armorStand.despawn(it) }
    }
    
    @Synchronized
    fun getChunkViewers(chunk: AsyncChunkPos): List<Player> {
        return chunkViewers[chunk] ?: emptyList()
    }
    
    @Synchronized
    fun changeArmorStandChunk(armorStand: FakeArmorStand, previousChunk: AsyncChunkPos, newChunk: AsyncChunkPos) {
        // move the armor stand to the new chunk key
        chunkArmorStands[previousChunk]!!.remove(armorStand)
        chunkArmorStands.getOrPut(newChunk) { mutableListOf() }.add(armorStand)
        
        // find all players that saw the old chunk but don't see the new one and despawn it for them
        val newChunksViewers = chunkViewers.getOrPut(newChunk) { mutableListOf() }
        chunkViewers[previousChunk]!!.stream()
            .filter { !newChunksViewers.contains(it) }
            .forEach { armorStand.despawn(it) }
    }
    
    @Synchronized
    private fun handleChunksChange(player: Player, newChunk: Chunk) {
        val currentChunks = visibleChunks[player] ?: emptySet()
        val newChunks = newChunk.pos.getInRange(4)
        
        // look for all chunks that are no longer visible
        currentChunks.stream()
            .filter { !newChunks.contains(it) }
            .forEach { chunk ->
                // despawn the armor stands there
                chunkArmorStands[chunk]?.forEach { armorStand ->
                    armorStand.despawn(player)
                }
                
                // remove the player from the viewer list
                chunkViewers[chunk]?.remove(player)
            }
        
        // look for all chunks weren't visible previously
        newChunks.stream()
            .filter { !currentChunks.contains(it) }
            .forEach { chunk ->
                // spawn the armor stands there
                chunkArmorStands[chunk]?.forEach { armorStand ->
                    armorStand.spawn(player)
                }
                
                // add the player to the viewers list
                chunkViewers.getOrPut(chunk) { mutableListOf() }.add(player)
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
    
    @EventHandler
    fun handleMove(event: PlayerMoveEvent) {
        val newChunk = event.to!!.chunk
        if (event.from.chunk != newChunk) {
            val player = event.player
            runAsyncTask { handleChunksChange(player, newChunk) }
        }
    }
    
    @EventHandler
    fun handleJoin(event: PlayerJoinEvent) {
        val player = event.player
        val chunk = player.location.chunk
        runAsyncTask { handleChunksChange(player, chunk) }
    }
    
    @EventHandler
    fun handleQuit(event: PlayerQuitEvent) {
        val player = event.player
        runAsyncTask { removeViewer(player) }
    }
    
}