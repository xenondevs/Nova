package xyz.xenondevs.nova.initialize

import xyz.xenondevs.nova.util.contentEquals
import xyz.xenondevs.nova.util.runTask
import java.util.concurrent.CountDownLatch

abstract class Initializable : Comparable<Initializable> {
    
    internal val latch = CountDownLatch(1)
    
    internal abstract val inMainThread: Boolean
    
    internal abstract val dependsOn: Set<Initializable>
    
    abstract fun init()
    
    open fun disable() = Unit
    
    fun initialize(parentLatch: CountDownLatch) {
        if (inMainThread) {
            runTask {
                init()
                this.latch.countDown()
                parentLatch.countDown()
            }
        } else {
            init()
            this.latch.countDown()
            parentLatch.countDown()
        }
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