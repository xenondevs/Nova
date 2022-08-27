package xyz.xenondevs.nova.util.concurrent

import java.util.concurrent.Semaphore

/**
 * A thread latch that can be turned on and off
 *
 * * on -> block awaiting thread
 * * off -> unblock awaiting thread
 */
class Latch {
    
    private val semaphore = Semaphore(1)
    
    fun on() = semaphore.acquire()
    
    fun off() = semaphore.release()
    
    fun await() {
        semaphore.acquire()
        semaphore.release()
    }
    
    fun state() = semaphore.availablePermits() == 0
    
    fun toggle() =
        if (state()) off() else on()
    
}