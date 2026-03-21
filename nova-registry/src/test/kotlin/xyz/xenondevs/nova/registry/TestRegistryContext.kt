package xyz.xenondevs.nova.registry

import net.kyori.adventure.key.Keyed
import xyz.xenondevs.commons.provider.Provider

class TestRegistryContext : RegistryContext {
    
    companion object {
        var inBootstrapPhase: Boolean = true
        val trackedEntries: MutableList<Provider<*>> = mutableListOf()
        val reloadListeners: MutableList<() -> Unit> = mutableListOf()
        
        fun reset() {
            inBootstrapPhase = true
            trackedEntries.clear()
            reloadListeners.clear()
        }
    }
    
    override val isInBootstrapPhase: Boolean
        get() = inBootstrapPhase
    
    override fun trackUnresolved(identifier: Keyed, entry: Provider<*>) {
        trackedEntries += entry
    }
    
    override fun registerPostTagReloadListener(listener: () -> Unit) {
        reloadListeners += listener
    }
    
}
