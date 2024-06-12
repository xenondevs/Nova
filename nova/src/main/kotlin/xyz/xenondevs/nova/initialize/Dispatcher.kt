package xyz.xenondevs.nova.initialize

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Defines how an [InitializerRunnable] is dispatched, i.e. on which thread it is executed.
 */
enum class Dispatcher(internal val dispatcher: CoroutineDispatcher?) {
    
    /**
     * The runnable is dispatched on the server thread.
     */
    SERVER(null),
    
    /**
     * The runnable is dispatched in an off-main-thread context,
     * in parallel with other runnables dispatched [ASYNC].
     */
    ASYNC(Dispatchers.Default)
    
}