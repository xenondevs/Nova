package xyz.xenondevs.nova.initialize

import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.util.contentEquals
import xyz.xenondevs.nova.util.runTask
import java.util.concurrent.CompletableFuture
import java.util.logging.Level

abstract class Initializable internal constructor() : Comparable<Initializable> {
    
    internal val initialization = CompletableFuture<Boolean>()
    internal var isInitialized = false
    
    internal abstract val inMainThread: Boolean
    internal abstract val dependsOn: Set<Initializable>
    internal abstract fun init()
    internal open fun disable() = Unit
    
    fun initialize() {
        fun performInitialization() {
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
        
        if (inMainThread)
            runTask(::performInitialization)
        else performInitialization()
    }
    
    override fun compareTo(other: Initializable): Int {
        val o1DependsOn = dependsOn
        val o2DependsOn = other.dependsOn
        
        if (o1DependsOn.isEmpty() && o2DependsOn.isEmpty())
            return 0 // Both depend on nothing
        if (o1DependsOn.isEmpty())
            return -1 // This depends on nothing, but other does
        if (o2DependsOn.isEmpty())
            return 1 // Other depends on nothing, but this does
        if (o1DependsOn.contentEquals(o2DependsOn))
            return 0 // Both depend on the same thing
        if (o1DependsOn.contains(other))
            return 1 // This depends on other
        if (o2DependsOn.contains(this))
            return -1 // Other depends on this
        return 0 // Both depend on different things
    }
    
    
}