package xyz.xenondevs.nova.initialize

import xyz.xenondevs.nova.LOGGER
import java.util.concurrent.CompletableFuture
import java.util.logging.Level

abstract class Initializable internal constructor() {
    
    internal val initialization = CompletableFuture<Boolean>()
    internal var isInitialized = false
    
    internal abstract val initializationStage: InitializationStage
    internal abstract val dependsOn: Set<Initializable>
    internal abstract fun init()
    internal open fun disable() = Unit
    
    fun initialize() {
        try {
            init()
            isInitialized = true
            Initializer.initialized += this
            initialization.complete(true)
        } catch (e: InitializationException) {
            LOGGER.severe(e.message)
        } catch (e: Exception) {
            LOGGER.log(Level.SEVERE, "An exception occurred trying to initialize $this", e)
        }
        
        initialization.complete(false)
    }
    
}