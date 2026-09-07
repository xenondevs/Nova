package xyz.xenondevs.nova.registry

import org.bukkit.Keyed

/**
 * A builder for a value of a [RegistryEntry].
 */
@RegistryElementBuilderDsl
sealed interface RegistryEntryBuilder<out T : RegistryEntry<*>> {
    
    /**
     * The entry of the element being built.
     */
    val entry: T
    
    /**
     * A builder for a value of a [RegistryEntry.Nova].
     */
    interface Nova<out T : NovaRegistryElement<T>> : RegistryEntryBuilder<RegistryEntry.Nova<T>> {
        
        /**
         * Adds the entry of the element being build to the given [tags].
         */
        fun tags(vararg tags: RegistryEntrySet.Nova.Tag<@UnsafeVariance T>)
        
    }
    
    /**
     * A builder for a value of a [RegistryEntry.Paper].
     */
    interface Paper<out T : Keyed> : RegistryEntryBuilder<RegistryEntry.Paper<T>> {
        
        /**
         * Adds the entry of the element being build to the given [tags].
         */
        fun tags(vararg tags: RegistryEntrySet.Paper.Tag<@UnsafeVariance T>)
        
    }
    
}