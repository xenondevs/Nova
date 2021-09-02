package xyz.xenondevs.nova.util

import java.util.concurrent.locks.ReentrantLock

fun ReentrantLock.tryLockAndRun(run: () -> Unit): Boolean {
    return if (tryLock()) {
        try {
            run()
            true
        } finally {
            unlock()
        }
    } else false
}

fun ReentrantLock.lockAndRun(run: () -> Unit) {
    lock()
    try {
        run()
    } finally {
        unlock()
    }
}