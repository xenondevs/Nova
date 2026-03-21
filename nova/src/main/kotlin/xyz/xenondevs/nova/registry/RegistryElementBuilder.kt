package xyz.xenondevs.nova.registry

import io.papermc.paper.registry.tag.TagKey
import net.minecraft.resources.RegistryOps

/**
 * A builder of a registry element, either [Nova] or [Vanilla].
 * You will only need to use this interface if you've created a custom [NovaRegistry] that you want
 * to load via [RegistryLoader].
 * Things like [NovaItemBuilder] intentionally do not implement this interface directly to hide
 * the build functions.
 */
sealed interface RegistryElementBuilder<out T : Any> {
    
    /**
     * Prepares the builder for build.
     * This function as called after the builder has been configured, but before the build function is called.
     * This allows for e.g. queuing asset generation in resource pack tasks, which will be done before the build function is called.
     */
    fun prepareBuild() = Unit
    
    /**
     * A builder of a [NovaRegistryElement].
     */
    interface Nova<out T : NovaRegistryElement<T>> : RegistryElementBuilder<T> {
        
        /**
         * Builds the registry element.
         */
        fun build(): T
        
    }
    
    /**
     * A builder of something that is registered in a vanilla registry.
     */
    interface Vanilla<out T : Any> : RegistryElementBuilder<T> {
        
        /**
         * Builds the registry element.
         * Can use [lookup] to get holders for other registry elements.
         */
        fun build(lookup: RegistryOps.RegistryInfoLookup): T
        
        /**
         * Builds a set of tags that the resulting element should be added to.
         */
        fun buildTagSet(): Set<TagKey<*>> = emptySet()
        
    }
    
}