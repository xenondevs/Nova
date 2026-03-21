package xyz.xenondevs.nova.registry

import io.papermc.paper.event.server.ServerResourcesReloadedEvent
import net.kyori.adventure.key.Keyed
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.IS_DEV_SERVER
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.initialize.InitializationException
import xyz.xenondevs.nova.util.registerEvents
import java.util.*

private data class TrackedEntry(
    val identifier: Keyed,
    val entry: Provider<*>,
    val origin: Throwable?
) {
    
    fun resolve(): Boolean {
        try {
            entry.get()
            return true
        } catch (_: NoSuchElementException) {
            return false
        }
    }
    
}

internal class NovaRegistryContext : RegistryContext {
    
    companion object : Listener {
        
        @Volatile
        private var isInBootstrapPhase = true
        private val trackedEntries: MutableList<TrackedEntry> = Collections.synchronizedList(ArrayList())
        private val reloadListeners: MutableList<() -> Unit> = Collections.synchronizedList(ArrayList())
        
        fun exitBootstrapPhase() {
            registerEvents()
            
            isInBootstrapPhase = false
            
            val unbound = trackedEntries.filterNot(TrackedEntry::resolve)
            if (unbound.isEmpty())
                return
            
            if (IS_DEV_SERVER) {
                throw InitializationException(
                    "Registry entries for non-existent values were created during bootstrap:\n"
                        + unbound.joinToString("\n") { (identifier, _, origin) ->
                        "- $identifier:\n ${origin!!.stackTraceToString().substringAfter('\n')}"
                    }
                )
            } else {
                throw InitializationException(
                    "Registry entries for non-existent values were created during bootstrap: "
                        + unbound.joinToString { it.identifier.toString() }
                        + " (enable dev mode to capture creation stack traces)"
                )
            }
        }
        
        @EventHandler
        private fun handleResourceReload(event: ServerResourcesReloadedEvent) {
            for (reloadListener in reloadListeners) {
                try {
                    reloadListener()
                } catch (e: Exception) {
                    LOGGER.error("An exception occurred while executing a post-tag-reload listener", e)
                }
            }
        }
        
    }
    
    override val isInBootstrapPhase: Boolean
        get() = Companion.isInBootstrapPhase
    
    override fun trackUnresolved(identifier: Keyed, entry: Provider<*>) {
        trackedEntries += TrackedEntry(identifier, entry, if (IS_DEV_SERVER) Throwable() else null)
    }
    
    override fun registerPostTagReloadListener(listener: () -> Unit) {
        reloadListeners += listener
    }
    
}