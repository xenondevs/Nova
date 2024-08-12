package xyz.xenondevs.nova.util.concurrent

import java.util.concurrent.CompletableFuture

/**
 * A [CompletableFuture] which combines the given [futures] using a logical AND.
 *
 * * The future will complete with true if all given futures were completed with true.
 * * The future will complete with false once one of the given futures was completed with false.
 *   All other futures will be cancelled.
 * * If one of the futures completes exceptionally, the future will complete exceptionally, using the same exception
 *   of the first future which completed exceptionally. All other futures will be cancelled.
 */
internal class CombinedBooleanFuture(private val futures: List<CompletableFuture<Boolean>>) : CompletableFuture<Boolean>() {
    
    init {
        futures.forEach { it.thenRun(::handleFutureArrival) }
    }
    
    private fun handleFutureArrival() {
        if (!isDone) {
            var allCompleted = true
            
            for (future in futures) {
                if (!future.isDone) {
                    // not all futures have been completed
                    allCompleted = false
                } else if (future.isCompletedExceptionally) {
                    // a future completed exceptionally
                    val ex = runCatching { future.get() }.exceptionOrNull()
                    completeExceptionally(ex)
                    futures.forEach { it.cancel(false) }
                    return
                } else if (!future.get()) {
                    // if this future is false, the results of the other futures can be ignored
                    complete(false)
                    futures.forEach { it.cancel(false) }
                    return
                }
            }
            
            if (allCompleted)
                complete(true)
        }
    }
    
}