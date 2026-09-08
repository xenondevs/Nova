package xyz.xenondevs.nova.registry

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.tag.TagKey
import org.bukkit.Keyed
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TestRegistryContext : RegistryContext {
    
    override val logger: Logger = LoggerFactory.getLogger(TestRegistryContext::class.java)
    
    companion object {
        var inBootstrapPhase: Boolean = true
        val trackedEntries = mutableListOf<TypedKey<*>>()
        val reloadListeners = mutableListOf<() -> Unit>()
        var scheduledDataReloads = 0
        
        fun reset() {
            inBootstrapPhase = true
            trackedEntries.clear()
            reloadListeners.clear()
            scheduledDataReloads = 0
        }
    }
    
    override val isInBootstrapPhase: Boolean
        get() = inBootstrapPhase
    
    override fun <T : Keyed> trackUnresolvedEntry(key: TypedKey<T>, registryAccess: RegistryAccess) {
        trackedEntries += key
    }
    
    override fun <T : Keyed> trackUnresolvedTag(key: TagKey<T>, registryAccess: RegistryAccess) {
    }
    
    override fun scheduleDataReload() {
        scheduledDataReloads++
    }
    
    override fun registerPostTagReloadListener(listener: () -> Unit) {
        reloadListeners += listener
    }
    
}
