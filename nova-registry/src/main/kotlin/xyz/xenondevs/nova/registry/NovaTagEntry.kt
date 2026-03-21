package xyz.xenondevs.nova.registry

/**
 * An entry in a pre-flattened Nova tag.
 */
sealed interface NovaTagEntry<T : NovaRegistryElement<T>> {
    
    /**
     * The registry this tag entry belongs to.
     */
    val registry: NovaRegistry<T>
    
    /**
     * A direct entry in a Nova tag, representing a single registry entry.
     */
    data class Direct<T : NovaRegistryElement<T>>(val entry: RegistryEntry.Nova<T>) : NovaTagEntry<T> {
        
        override val registry: NovaRegistry<T>
            get() = entry.registry
        
    }
    
    /**
     * A tag entry in a Nova tag, representing another tag that is included in this tag.
     */
    data class Tag<T : NovaRegistryElement<T>>(val tag: RegistryEntrySet.Nova.Tag<T>) : NovaTagEntry<T> {
        
        override val registry: NovaRegistry<T>
            get() = tag.registry
        
    }
    
}