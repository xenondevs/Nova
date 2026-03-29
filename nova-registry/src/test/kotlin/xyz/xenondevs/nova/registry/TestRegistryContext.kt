package xyz.xenondevs.nova.registry

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.tag.TagKey
import org.bukkit.Keyed

class TestRegistryContext : RegistryContext {
    
    companion object {
        var inBootstrapPhase: Boolean = true
        val trackedEntries = mutableListOf<TypedKey<*>>()
        val reloadListeners = mutableListOf<() -> Unit>()
        
        fun reset() {
            inBootstrapPhase = true
            trackedEntries.clear()
            reloadListeners.clear()
        }
    }
    
    override val isInBootstrapPhase: Boolean
        get() = inBootstrapPhase
    
    override fun <T : Keyed> trackUnresolvedEntry(key: TypedKey<T>, registryAccess: RegistryAccess) {
        trackedEntries += key
    }
    
    override fun <N : NovaRegistryElement<N>, T : Keyed> trackUnresolvedEntry(key: TypedKey<T>, novaRegistry: NovaRegistry<N>, registryAccess: RegistryAccess) {
    }
    
    override fun <T : Keyed> trackUnresolvedTag(key: TagKey<T>, registryAccess: RegistryAccess) {
    }
    
    override fun <N : NovaRegistryElement<N>, T : Keyed> trackUnresolvedTag(key: TagKey<T>, novaRegistry: NovaRegistry<N>, registryAccess: RegistryAccess) {
    }
    
    override fun registerPostTagReloadListener(listener: () -> Unit) {
        reloadListeners += listener
    }
    
}
