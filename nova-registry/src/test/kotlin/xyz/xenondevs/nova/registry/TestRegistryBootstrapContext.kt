package xyz.xenondevs.nova.registry

class TestRegistryBootstrapContext : RegistryBootstrapContext {

    companion object {
        var inBootstrapPhase: Boolean = true
        val trackedEntries: MutableList<RegistryEntry<*>> = mutableListOf()

        fun reset() {
            inBootstrapPhase = true
            trackedEntries.clear()
        }
    }

    override val isInBootstrapPhase: Boolean
        get() = inBootstrapPhase

    override fun trackUnresolvedEntry(entry: RegistryEntry<*>) {
        trackedEntries += entry
    }

}
