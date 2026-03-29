package xyz.xenondevs.nova.registry

import io.papermc.paper.event.server.ServerResourcesReloadedEvent
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.tag.TagKey
import org.bukkit.Keyed
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import xyz.xenondevs.nova.IS_DEV_SERVER
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.initialize.InitializationException
import xyz.xenondevs.nova.util.registerEvents
import java.util.*

private sealed interface Tracked {
    
    val origin: Throwable?
    
    fun isBound(): Boolean
    
    class Entry<T : Keyed>(
        private val key: TypedKey<T>,
        private val registryAccess: RegistryAccess,
        override val origin: Throwable?
    ) : Tracked {
        override fun isBound() = registryAccess.getRegistry(key.registryKey()).get(key) != null
        override fun toString() = key.registryKey().key().asString() + "/" + key.asString()
    }
    
    class EitherEntry<N : NovaRegistryElement<N>, T : Keyed>(
        private val key: TypedKey<T>,
        private val novaRegistry: NovaRegistry<N>,
        private val registryAccess: RegistryAccess,
        override val origin: Throwable?
    ) : Tracked {
        
        override fun isBound(): Boolean =
            novaRegistry.getOptional(key).get() != null ||
                registryAccess.getRegistry(key.registryKey()).get(key) != null
        
        override fun toString(): String =
            novaRegistry.key.asString() +
                "|" + key.registryKey().key().asString() +
                "/" + key.asString()
        
    }
    
    class Tag<T : Keyed>(
        private val key: TagKey<T>,
        private val registryAccess: RegistryAccess,
        override val origin: Throwable?
    ) : Tracked {
        override fun isBound() = registryAccess.getRegistry(key.registryKey()).hasTag(key)
        override fun toString() = key.registryKey().key().asString() + "/#" + key.key().asString()
    }
    
    class EitherTag<N : NovaRegistryElement<N>, T : Keyed>(
        private val key: TagKey<T>,
        private val novaRegistry: NovaRegistry<N>,
        private val registryAccess: RegistryAccess,
        override val origin: Throwable?
    ) : Tracked {
        
        override fun isBound(): Boolean =
            novaRegistry.getOptionalTag(key.key()).get() != null ||
                registryAccess.getRegistry(key.registryKey()).hasTag(key)
        
        override fun toString(): String =
            novaRegistry.key.asString() +
                "|" + key.registryKey().key().asString() + 
                "/#" + key.key().asString()
    
    }
    
}

internal class NovaRegistryContext : RegistryContext {
    
    companion object : Listener {
        
        @Volatile
        private var isInBootstrapPhase = true
        private val trackedEntries: MutableList<Tracked> = Collections.synchronizedList(ArrayList())
        private val reloadListeners: MutableList<() -> Unit> = Collections.synchronizedList(ArrayList())
        
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
    
    override fun <N : NovaRegistryElement<N>, T : Keyed> trackUnresolvedEntry(key: TypedKey<T>, novaRegistry: NovaRegistry<N>, registryAccess: RegistryAccess) {
        trackedEntries += Tracked.EitherEntry(key, novaRegistry, registryAccess, if (IS_DEV_SERVER) Throwable() else null)
    }
    
    override fun <T : Keyed> trackUnresolvedTag(key: TagKey<T>, registryAccess: RegistryAccess) {
        trackedEntries += Tracked.Tag(key, registryAccess, if (IS_DEV_SERVER) Throwable() else null)
    }
    
    override fun <N : NovaRegistryElement<N>, T : Keyed> trackUnresolvedTag(key: TagKey<T>, novaRegistry: NovaRegistry<N>, registryAccess: RegistryAccess) {
        trackedEntries += Tracked.EitherTag(key, novaRegistry, registryAccess, if (IS_DEV_SERVER) Throwable() else null)
    }
    
    override fun registerPostTagReloadListener(listener: () -> Unit) {
        reloadListeners += listener
    }
    
}