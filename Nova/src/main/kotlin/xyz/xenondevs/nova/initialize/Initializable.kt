package xyz.xenondevs.nova.initialize

import xyz.xenondevs.nova.util.runTask
import java.util.concurrent.CountDownLatch

abstract class Initializable : Comparable<Initializable> {
    
    val latch = CountDownLatch(1)
    
    abstract val inMainThread: Boolean
    
    abstract val dependsOn: Initializable?
    
    abstract fun init()
    
    open fun initialize(parentLatch: CountDownLatch) {
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
        
        if (o1DependsOn == null && o2DependsOn == null)
            return 0 // Both depend on nothing
        if (o1DependsOn == null)
            return -1 // This depends on nothing, but other does
        if (o2DependsOn == null)
            return 1 // Other depends on nothing, but this does
        if (o1DependsOn == o2DependsOn)
            return 0 // Both depend on the same thing
        if (o1DependsOn == other)
            return 1 // This depends on other
        if (o2DependsOn == this)
            return -1 // Other depends on this
        return 0 // Both depend on different things
    }
    
    
}