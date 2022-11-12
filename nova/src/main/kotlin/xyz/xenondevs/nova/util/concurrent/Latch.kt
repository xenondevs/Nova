package xyz.xenondevs.nova.util.concurrent

import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * A thread latch that can be turned on and off
 *
 * * close -> block awaiting thread
 * * open -> unblock awaiting thread
 */
internal class Latch {
    
    private val semaphore = Semaphore(1)
    
    fun close() = semaphore.acquire()
    
    fun open() = semaphore.release()
    
    fun await() {
        semaphore.acquire()
        semaphore.release()
    }
    
    fun await(timeout: Long, unit: TimeUnit): Boolean {
        if (semaphore.tryAcquire(timeout, unit)) {
            semaphore.release()
            return true
        }
        
        return false
    }
    
    fun isClosed(): Boolean =
        semaphore.availablePermits() == 0
    
    fun toggle() =
        if (isClosed()) open() else close()
    
}