package xyz.xenondevs.nova.registry

import net.minecraft.resources.RegistryOps
import org.bukkit.Keyed
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.provider

/**
 * A builder of a registry element, either [Nova] or [Vanilla].
 * You will only need to use this interface if you've created a custom [NovaRegistry] that you want
 * to load via [RegistryLoader].
 * Things like [NovaItemBuilder] intentionally do not implement this interface directly to hide
 * the build functions.
 */
sealed interface RegistryElementBuilder<out T : Keyed> {
    
    /**
     * A reloadable set of tags that the element built by this builder belongs in.
     * Can be updated at any time (independently of registry reloading / re-running).
     */
    val tags: Provider<Set<RegistryEntrySet.Paper.Tag<T>>>
        get() = provider(emptySet())
    
    /**
     * Prepares the builder for build.
     * This function is called after the builder has been configured, but before the build function is called.
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
        
        companion object {
            
            /**
             * Creates an anonymous element builder for [entry] that creates an intermediary
             * result in [prepare] which is then used to [build] the final element.
             */
            fun <T : NovaRegistryElement<T>, I : Any> anonymous(
                entry: RegistryEntry.Nova<T>,
                prepare: (RegistryEntry.Nova<T>) -> I,
                build: (RegistryEntry.Nova<T>, I) -> T
            ): Nova<T> = object : Nova<T> {
                
                private lateinit var prep: I
                
                override fun prepareBuild() {
                    prep = prepare(entry)
                }
                
                override fun build(): T = build(entry, prep)
                
            }
            
        }
        
    }
    
    /**
     * A builder of something that is registered in a vanilla registry.
     */
    interface Vanilla<out API : Keyed, out NMS : Any> : RegistryElementBuilder<API> {
        
        /**
         * Builds the registry element.
         * Can use [lookup] to get holders for other registry elements.
         */
        fun build(lookup: RegistryOps.RegistryInfoLookup): NMS
        
    }
    
    /**
     * A [Vanilla] builder that can be reset and re-run. How updates are propagated to the elements
     * is left to the implementation. [RegistryElementBuilder.Vanilla.prepareBuild] is called 
     * again on re-run, but [RegistryElementBuilder.Vanilla.build] is not.
     * Intended for non-reloadable vanilla registries.
     */
    interface RerunnableVanilla<out API : Keyed, out NMS : Any> : Vanilla<API, NMS> {
        
        /**
         * Resets the builder to its initial state.
         * Called immediately before re-running configuration on the builder.
         */
        fun reset()
        
    }
    
}