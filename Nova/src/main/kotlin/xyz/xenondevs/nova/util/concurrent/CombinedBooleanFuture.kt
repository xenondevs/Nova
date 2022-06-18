package xyz.xenondevs.nova.util.concurrent

import java.util.concurrent.CompletableFuture

class CombinedBooleanFuture(private val futures: List<CompletableFuture<Boolean>>) : CompletableFuture<Boolean>() {
    
    init {
        futures.forEach { it.thenRun(::handleFutureArrival) }
    }
    
    constructor(vararg futures: CompletableFuture<Boolean>) : this(futures.asList())
    
    private fun handleFutureArrival() {
        if (!isDone && canCombine()) {
            complete(getCombined())
        }
    }
    
    private fun canCombine(): Boolean {
        var allDone = true
        futures.forEach { future ->
            if (!future.isDone) {
                // not all futures are done
                allDone = false
            } else if (!future.get()) {
                // if this future is false, the results of the other futures can be ignored
                return true
            }
        }
        
        return allDone
    }
    
    private fun getCombined(): Boolean {
        futures.forEach { future ->
            // if the future is not done but this method was called, at least one of the futures is false
            if (!future.isDone || !future.get())
                return false
        }
        
        return true
    }
    
}