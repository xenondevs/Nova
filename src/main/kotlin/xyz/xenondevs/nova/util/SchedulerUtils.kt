package xyz.xenondevs.nova.util

import org.bukkit.Bukkit
import xyz.xenondevs.nova.NOVA

fun runTaskLater(delay: Long, run: () -> Unit) =
    Bukkit.getScheduler().runTaskLater(NOVA, run, delay)

fun runTask(run: () -> Unit) =
    Bukkit.getScheduler().runTask(NOVA, run)

fun runTaskTimer(delay: Long, period: Long, run: () -> Unit) =
    Bukkit.getScheduler().runTaskTimer(NOVA, run, delay, period)

fun runAsyncTask(run: () -> Unit) =
    Bukkit.getScheduler().runTaskAsynchronously(NOVA, run)

fun runAsyncTaskLater(delay: Long, run: () -> Unit) =
    Bukkit.getScheduler().runTaskLaterAsynchronously(NOVA, run, delay)

fun runAsyncTaskTimer(delay: Long, period: Long, run: () -> Unit) =
    Bukkit.getScheduler().runTaskTimerAsynchronously(NOVA, run, delay, period)

fun runSyncTaskWhenUnlocked(lock: ObservableLock, run: () -> Unit) {
    runTaskLater(1) { if (!lock.tryLockAndRun(run)) runSyncTaskWhenUnlocked(lock, run) }
}

fun runTaskSynchronized(lock: Any, run: () -> Unit) {
    Bukkit.getScheduler().runTask(NOVA, Runnable { synchronized(lock) { run() } })
}

fun runTaskLaterSynchronized(lock: Any, delay: Long, run: () -> Unit) {
    Bukkit.getScheduler().runTaskLater(NOVA, Runnable { synchronized(lock) { run() } }, delay)
}

fun runTaskTimerSynchronized(lock: Any, delay: Long, period: Long, run: () -> Unit) {
    Bukkit.getScheduler().runTaskTimer(NOVA, Runnable { synchronized(lock) { run() } }, delay, period)
}
