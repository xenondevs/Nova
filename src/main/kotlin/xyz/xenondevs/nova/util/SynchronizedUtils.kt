package xyz.xenondevs.nova.util

import java.util.concurrent.locks.ReentrantLock

fun ObservableLock.tryLockAndRun(run: () -> Unit): Boolean {
    return if (tryLock()) {
        try {
            run()
            true
        } finally {
            unlock()
        }
    } else false
}

fun ObservableLock.lockAndRun(run: () -> Unit) {
    lock()
    try {
        run()
    } finally {
        unlock()
    }
}

class ObservableLock : ReentrantLock() {
    
    public override fun getOwner(): Thread? {
        return super.getOwner()
    }
    
}