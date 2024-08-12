package xyz.xenondevs.nova.initialize

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Defines how an [InitializerRunnable] is dispatched.
 */
enum class Dispatcher(internal val dispatcher: CoroutineDispatcher?) {
    
    /**
     * The initialization is performed synchronously with other initializables.
     */
    SYNC(null),
    
    /**
     * The initialization is performed asynchronously, in parallel with other async initializables.
     */
    ASYNC(Dispatchers.Default)
    
}