package xyz.xenondevs.nova.registry

import org.bukkit.Keyed

/**
 * A builder for a value of a [RegistryEntry].
 */
@RegistryElementBuilderDsl
sealed interface RegistryEntryBuilder<T : RegistryEntry<*>> {
    
    /**
     * The entry of the element being built.
     */
    val entry: T
    
    /**
     * A builder for a value of a [RegistryEntry.Nova].
     */
    interface Nova<T : NovaRegistryElement<T>> : RegistryEntryBuilder<RegistryEntry.Nova<T>>
    
    /**
     * A builder for a value of a [RegistryEntry.Paper].
     */
    interface Paper<T : Keyed> : RegistryEntryBuilder<RegistryEntry.Paper<T>>
    
}