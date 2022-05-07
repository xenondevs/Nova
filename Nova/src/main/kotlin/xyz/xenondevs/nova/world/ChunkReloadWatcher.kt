package xyz.xenondevs.nova.world

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.initialize.Initializable
import java.util.logging.Level

object ChunkReloadWatcher : Initializable(), Listener {
    
    private const val RELOAD_TIME_LIMIT = 500
    private const val RELOAD_LIMIT = 2
    
    private val CHUNK_LOADS = HashMap<ChunkPos, Pair<Long, Int>>()
    
    override val inMainThread = false
    override val dependsOn = emptySet<Initializable>()
    
    override fun init() {
        if (DEFAULT_CONFIG.getBoolean("debug.watch_chunk_reloads"))
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
                LOGGER.log(
                    Level.INFO,
                    "(This is not an error, you can disable this message in plugins/Nova/config/config.yml)" +
                        "Nova has detected a Chunk loading multiple times in a short timeframe." +
                        "$pos | Reload #$reloadAmount" +
                        "A stacktrace is attached for debugging purposes:",
                    Exception()
                )
            }
        } else CHUNK_LOADS[pos] = currentTime to 1
    }
    
}