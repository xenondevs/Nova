package xyz.xenondevs.nova.world

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.initialize.Initializable

object ChunkReloadWatcher : Initializable(), Listener {
    
    private const val RELOAD_TIME_LIMIT = 500
    private const val RELOAD_LIMIT = 2
    
    private val CHUNK_LOADS = HashMap<ChunkPos, Pair<Long, Int>>()
    
    override val inMainThread = false
    override val dependsOn = emptySet<Initializable>()
    
    override fun init() {
        if (DEFAULT_CONFIG.getBoolean("chunk_reload_watcher"))
            Bukkit.getPluginManager().registerEvents(this, NOVA)
    }
    
    @EventHandler
    fun handleChunkLoad(event: ChunkLoadEvent) {
        val pos = event.chunk.pos
        val currentTime = System.currentTimeMillis()
        
        val pair = CHUNK_LOADS[pos]
        if (pair != null && currentTime - pair.first <= RELOAD_TIME_LIMIT) {
            val reloadAmount = pair.second + 1
            CHUNK_LOADS[pos] = currentTime to reloadAmount
            
            if (reloadAmount >= RELOAD_LIMIT) {
                ChunkReloadException(pos, reloadAmount).printStackTrace()
            }
        } else CHUNK_LOADS[pos] = currentTime to 1
    }
    
}

private class ChunkReloadException(pos: ChunkPos, amount: Int) : Exception("""
    (This is not an error, you can disable this message in plugins/Nova/config/config.json)
    Nova has detected a Chunk loading multiple times in a short timeframe.
    $pos | Reload #$amount
    A stacktrace is attached for debugging purposes:
    """.trimMargin())