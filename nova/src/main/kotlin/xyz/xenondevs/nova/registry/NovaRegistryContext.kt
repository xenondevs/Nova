package xyz.xenondevs.nova.registry

import io.papermc.paper.event.server.ServerResourcesReloadedEvent
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.tag.TagKey
import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import xyz.xenondevs.nova.IS_DEV_SERVER
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.initialize.InitializationException
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.runTask
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

private sealed interface Tracked {
    
    val origin: Throwable?
    
    fun isBound(): Boolean
    
    class Entry<T : Keyed>(
        private val key: TypedKey<T>,
        private val registryAccess: RegistryAccess,
        override val origin: Throwable?
    ) : Tracked {
        override fun isBound() = registryAccess.getRegistry(key.registryKey()).get(key) != null
        override fun toString() = key.registryKey().key().asString() + "/" + key.key().asString()
    }
    
    class Tag<T : Keyed>(
        private val key: TagKey<T>,
        private val registryAccess: RegistryAccess,
        override val origin: Throwable?
    ) : Tracked {
        override fun isBound() = registryAccess.getRegistry(key.registryKey()).hasTag(key)
        override fun toString() = key.registryKey().key().asString() + "/#" + key.key().asString()
    }
    
}

internal class NovaRegistryContext : RegistryContext {
    
    companion object : Listener {
        
        @Volatile
        private var isInBootstrapPhase = true
        private val trackedEntries: MutableList<Tracked> = Collections.synchronizedList(ArrayList())
        private val reloadListeners: MutableList<() -> Unit> = Collections.synchronizedList(ArrayList())
        private val dataReloadScheduled = AtomicBoolean()
        
        fun exitBootstrapPhase() {
            registerEvents()
            
            isInBootstrapPhase = false
            
            val unbound = trackedEntries.filterNot(Tracked::isBound)
            if (unbound.isEmpty()) {
                trackedEntries.clear()
                return
            }
            
            if (IS_DEV_SERVER) {
                throw InitializationException(
                    "Registry entries for non-existent values were created during bootstrap:\n"
                        + unbound.joinToString("\n") { tracked ->
                        "- $tracked:\n ${tracked.origin!!.stackTraceToString().substringAfter('\n')}"
                    }
                )
            } else {
                throw InitializationException(
                    "Registry entries for non-existent values were created during bootstrap: "
                        + unbound.joinToString()
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
    
    override fun <T : Keyed> trackUnresolvedEntry(key: TypedKey<T>, registryAccess: RegistryAccess) {
        trackedEntries += Tracked.Entry(key, registryAccess, if (IS_DEV_SERVER) Throwable() else null)
    }
    
    override fun <T : Keyed> trackUnresolvedTag(key: TagKey<T>, registryAccess: RegistryAccess) {
        trackedEntries += Tracked.Tag(key, registryAccess, if (IS_DEV_SERVER) Throwable() else null)
    }
    
    override fun scheduleDataReload() {
        if (isInBootstrapPhase || !dataReloadScheduled.compareAndSet(false, true))
            return
        
        runTask {
            dataReloadScheduled.set(false)
            Bukkit.reloadData()
        }
    }
    
    override fun registerPostTagReloadListener(listener: () -> Unit) {
        reloadListeners += listener
    }
    
}