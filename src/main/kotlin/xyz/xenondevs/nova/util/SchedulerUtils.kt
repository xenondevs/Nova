@file:Suppress("NOTHING_TO_INLINE")

package xyz.xenondevs.nova.util

import org.bukkit.Bukkit
import xyz.xenondevs.nova.NOVA

inline fun runTaskLater(delay: Long, noinline run: () -> Unit) =
    Bukkit.getScheduler().runTaskLater(NOVA, run, delay)

inline fun runTask(noinline run: () -> Unit) =
    Bukkit.getScheduler().runTask(NOVA, run)

inline fun runTaskTimer(delay: Long, period: Long, noinline run: () -> Unit) =
    Bukkit.getScheduler().runTaskTimer(NOVA, run, delay, period)

inline fun runAsyncTask(noinline run: () -> Unit) =
    Bukkit.getScheduler().runTaskAsynchronously(NOVA, run)

inline fun runAsyncTaskSynchronized(lock: Any, noinline run: () -> Unit) =
    Bukkit.getScheduler().runTaskAsynchronously(NOVA, Runnable { synchronized(lock, run) })

inline fun runAsyncTaskTimerSynchronized(lock: Any, delay: Long, period: Long, noinline run: () -> Unit) =
    Bukkit.getScheduler().runTaskTimerAsynchronously(NOVA, Runnable { synchronized(lock, run) }, delay, period)

inline fun runAsyncTaskWithLock(lock: ObservableLock, noinline run: () -> Unit) =
    Bukkit.getScheduler().runTaskAsynchronously(NOVA, Runnable { lock.lockAndRun(run) })

inline fun runAsyncTaskLater(delay: Long, noinline run: () -> Unit) =
    Bukkit.getScheduler().runTaskLaterAsynchronously(NOVA, run, delay)

inline fun runAsyncTaskTimer(delay: Long, period: Long, noinline run: () -> Unit) =
    Bukkit.getScheduler().runTaskTimerAsynchronously(NOVA, run, delay, period)

inline fun runTaskSynchronized(lock: Any, noinline run: () -> Unit) =
    Bukkit.getScheduler().runTask(NOVA, Runnable { synchronized(lock, run) })

inline fun runTaskLaterSynchronized(lock: Any, delay: Long, noinline run: () -> Unit) =
    Bukkit.getScheduler().runTaskLater(NOVA, Runnable { synchronized(lock, run) }, delay)

inline fun runTaskTimerSynchronized(lock: Any, delay: Long, period: Long, noinline run: () -> Unit) =
    Bukkit.getScheduler().runTaskTimer(NOVA, Runnable { synchronized(lock, run) }, delay, period)

fun runSyncTaskWhenUnlocked(lock: ObservableLock, run: () -> Unit) {
    runTaskLater(1) { if (!lock.tryLockAndRun(run)) runSyncTaskWhenUnlocked(lock, run) }
}
