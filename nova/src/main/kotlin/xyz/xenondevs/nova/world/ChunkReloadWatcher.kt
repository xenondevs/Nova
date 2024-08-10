package xyz.xenondevs.nova.world

import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.initialize.Dispatcher
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.util.registerEvents
import java.util.logging.Level

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dispatcher = Dispatcher.ASYNC
)
internal object ChunkReloadWatcher : Listener {
    
    private const val RELOAD_TIME_LIMIT = 500
    private const val RELOAD_LIMIT = 2
    
    private val CHUNK_LOADS = HashMap<ChunkPos, Pair<Long, Int>>()
    
    @InitFun
    private fun init() {
        val enabled = MAIN_CONFIG.entry<Boolean>("debug", "watch_chunk_reloads")
        enabled.subscribe(::reload)
        reload(enabled.get())
    }
    
    private fun reload(enabled: Boolean) {
        HandlerList.unregisterAll(this)
        if (enabled) registerEvents()
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