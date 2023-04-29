package xyz.xenondevs.nova.hook

import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass

/**
 * An annotation to mark a class as a hook.
 * @param plugins The names of the plugins that this hook works with.
 * @param requireAll Whether all plugins in [plugins] have to be loaded for this hook to be loaded.
 * @param loadListener The [LoadListener] that is used to wait for the plugin to finish loading.
 */
@Target(AnnotationTarget.CLASS)
annotation class Hook(
    val plugins: Array<String>, 
    val unless: Array<String> = [], 
    val requireAll: Boolean = false,
    val loadListener: KClass<out LoadListener> = LoadListener::class
)

/**
 * A listener that is used to wait for a plugin to finish loading.
 * 
 * @see AwaitLoaded
 */
interface LoadListener {
    
    /**
     * A [CompletableFuture] that is completed when the plugin is loaded or failed to load.
     */
    val loaded: CompletableFuture<Boolean>
    
}