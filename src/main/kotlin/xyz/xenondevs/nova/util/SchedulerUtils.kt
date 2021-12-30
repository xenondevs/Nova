@file:Suppress("NOTHING_TO_INLINE")

package xyz.xenondevs.nova.util

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.bukkit.Bukkit
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import java.util.concurrent.*

val USE_NOVA_SCHEDULER = DEFAULT_CONFIG.getBoolean("nova_executor.enabled")

inline fun runTaskLater(delay: Long, noinline run: () -> Unit) =
    Bukkit.getScheduler().runTaskLater(NOVA, run, delay)

inline fun runTask(noinline run: () -> Unit) =
    Bukkit.getScheduler().runTask(NOVA, run)

inline fun runTaskTimer(delay: Long, period: Long, noinline run: () -> Unit) =
    Bukkit.getScheduler().runTaskTimer(NOVA, run, delay, period)

inline fun runTaskSynchronized(lock: Any, noinline run: () -> Unit) =
    Bukkit.getScheduler().runTask(NOVA, Runnable { synchronized(lock, run) })

inline fun runTaskLaterSynchronized(lock: Any, delay: Long, noinline run: () -> Unit) =
    Bukkit.getScheduler().runTaskLater(NOVA, Runnable { synchronized(lock, run) }, delay)

inline fun runTaskTimerSynchronized(lock: Any, delay: Long, period: Long, noinline run: () -> Unit) =
    Bukkit.getScheduler().runTaskTimer(NOVA, Runnable { synchronized(lock, run) }, delay, period)

fun runSyncTaskWhenUnlocked(lock: ObservableLock, run: () -> Unit) {
    runTaskLater(1) { if (!lock.tryLockAndRun(run)) runSyncTaskWhenUnlocked(lock, run) }
}

inline fun runAsyncTask(noinline run: () -> Unit) {
    if (USE_NOVA_SCHEDULER) AsyncExecutor.runAsync(run)
    else Bukkit.getScheduler().runTaskAsynchronously(NOVA, run)
}

inline fun runAsyncTaskSynchronized(lock: Any, noinline run: () -> Unit) {
    val task = { synchronized(lock, run) }
    if (USE_NOVA_SCHEDULER) AsyncExecutor.runAsync(task)
    else Bukkit.getScheduler().runTaskAsynchronously(NOVA, task)
}

inline fun runAsyncTaskWithLock(lock: ObservableLock, noinline run: () -> Unit) {
    val task = { lock.lockAndRun(run) }
    if (USE_NOVA_SCHEDULER) AsyncExecutor.runAsync(task)
    else Bukkit.getScheduler().runTaskAsynchronously(NOVA, task)
}

inline fun runAsyncTaskTimerSynchronized(lock: Any, delay: Long, period: Long, noinline run: () -> Unit) =
    Bukkit.getScheduler().runTaskTimerAsynchronously(NOVA, Runnable { synchronized(lock, run) }, delay, period)

inline fun runAsyncTaskTimer(delay: Long, period: Long, noinline run: () -> Unit) =
    Bukkit.getScheduler().runTaskTimerAsynchronously(NOVA, run, delay, period)

object AsyncExecutor {
    
    private val MIN_THREADS = DEFAULT_CONFIG.getInt("nova_executor.min_threads")!!
    private val MAX_THREADS = DEFAULT_CONFIG.getInt("nova_executor.max_threads")!!
    private val THREAD_TIMEOUT = DEFAULT_CONFIG.getLong("nova_executor.thread_timeout")!!
    
    private lateinit var threadFactory: ThreadFactory
    private lateinit var executorService: ExecutorService
    
    init {
        if (USE_NOVA_SCHEDULER) {
            threadFactory = ThreadFactoryBuilder().setNameFormat("Async Nova Worker - %d").build()
            executorService = ThreadPoolExecutor(MIN_THREADS, MAX_THREADS, THREAD_TIMEOUT, TimeUnit.MILLISECONDS, LinkedBlockingQueue(), threadFactory)
        }
    }
    
    fun runAsync(task: () -> Unit): Future<*> =
        executorService.submit(task)
    
    fun shutdown() {
        if (USE_NOVA_SCHEDULER) {
            executorService.shutdown()
        }
    }
    
}
