package xyz.xenondevs.nova.registry

import xyz.xenondevs.nova.IS_DEV_SERVER
import xyz.xenondevs.nova.initialize.InitializationException
import java.util.*

private data class TrackedEntry(
    val entry: RegistryEntry<*>,
    val origin: Throwable?
) {
    
    fun validate(): Boolean {
        try {
            entry.get()
            return true
        } catch (_: NoSuchElementException) {
            return false
        }
    }
    
}

internal class NovaRegistryBootstrapContext : RegistryBootstrapContext {
    
    companion object {
        
        @Volatile
        private var isInBootstrapPhase = true
        private val trackedEntries: MutableList<TrackedEntry> = Collections.synchronizedList(ArrayList())
        
        fun exitBootstrapPhase() {
            isInBootstrapPhase = false
            
            val unbound = trackedEntries.filterNot(TrackedEntry::validate)
            if (unbound.isEmpty())
                return
            
            if (IS_DEV_SERVER) {
                throw InitializationException(
                    "Registry entries for non-existent values were created during bootstrap:\n"
                        + unbound.joinToString("\n") { (entry, origin) ->
                        "- $entry:\n ${origin!!.stackTraceToString().substringAfter('\n')}"
                    }
                )
            } else {
                throw InitializationException(
                    "Registry entries for non-existent values were created during bootstrap: "
                        + unbound.joinToString { it.entry.toString() }
                        + " (enable dev mode to capture creation stack traces)"
                )
            }
        }
        
    }
    
    override val isInBootstrapPhase: Boolean
        get() = Companion.isInBootstrapPhase
    
    override fun trackUnresolvedEntry(entry: RegistryEntry<*>) {
        trackedEntries += TrackedEntry(entry, if (IS_DEV_SERVER) Throwable() else null)
    }
    
}