package xyz.xenondevs.nova.util.concurrent

import java.util.concurrent.CompletableFuture

class CombinedBooleanFuture(private val futures: List<CompletableFuture<Boolean>>) : CompletableFuture<Boolean>() {
    
    init {
        futures.forEach { it.thenRun(::handleFutureArrival) }
    }
    
    constructor(vararg futures: CompletableFuture<Boolean>) : this(futures.asList())
    
    private fun handleFutureArrival() {
        if (!isDone) {
            var allDone = true
            futures.forEach { future ->
                if (!future.isDone) {
                    // not all futures are done
                    allDone = false
                } else if (!future.get()) {
                    // if this future is false, the results of the other futures can be ignored
                    complete(false)
                    return
                }
            }
            
            if (allDone)
                complete(true)
        }
    }
    
}