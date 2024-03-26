package xyz.xenondevs.nova.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import org.bukkit.Bukkit
import xyz.xenondevs.nova.NOVA_PLUGIN
import xyz.xenondevs.nova.util.concurrent.isServerThread
import kotlin.coroutines.CoroutineContext

object BukkitDispatcher : CoroutineDispatcher() {
    
    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        return !isServerThread
    }
    
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        Bukkit.getScheduler().runTask(NOVA_PLUGIN, block)
    }
    
}