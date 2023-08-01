package xyz.xenondevs.nova.util

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.bukkit.Bukkit
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.NOVA_PLUGIN
import xyz.xenondevs.nova.data.config.MAIN_CONFIG
import xyz.xenondevs.nova.data.config.entry
import xyz.xenondevs.nova.util.concurrent.ObservableLock
import xyz.xenondevs.nova.util.concurrent.lockAndRun
import xyz.xenondevs.nova.util.concurrent.tryLockAndRun
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.logging.Level

val USE_NOVA_SCHEDULER by MAIN_CONFIG.entry<Boolean>("performance", "nova_executor", "enabled")

fun runTaskLater(delay: Long, run: () -> Unit) =
    Bukkit.getScheduler().runTaskLater(NOVA_PLUGIN, run, delay)

fun runTask(run: () -> Unit) =
    Bukkit.getScheduler().runTask(NOVA_PLUGIN, run)

fun runTaskTimer(delay: Long, period: Long, run: () -> Unit) =
    Bukkit.getScheduler().runTaskTimer(NOVA_PLUGIN, run, delay, period)

fun runTaskSynchronized(lock: Any, run: () -> Unit) =
    Bukkit.getScheduler().runTask(NOVA_PLUGIN, Runnable { synchronized(lock, run) })

fun runTaskLaterSynchronized(lock: Any, delay: Long, run: () -> Unit) =
    Bukkit.getScheduler().runTaskLater(NOVA_PLUGIN, Runnable { synchronized(lock, run) }, delay)

fun runTaskTimerSynchronized(lock: Any, delay: Long, period: Long, run: () -> Unit) =
    Bukkit.getScheduler().runTaskTimer(NOVA_PLUGIN, Runnable { synchronized(lock, run) }, delay, period)

fun runSyncTaskWhenUnlocked(lock: ObservableLock, run: () -> Unit) {
    runTaskLater(1) { if (!lock.tryLockAndRun(run)) runSyncTaskWhenUnlocked(lock, run) }
}

fun runAsyncTask(run: () -> Unit) {
    if (USE_NOVA_SCHEDULER) AsyncExecutor.run(run)
    else Bukkit.getScheduler().runTaskAsynchronously(NOVA_PLUGIN, run)
}

fun runAsyncTaskLater(delay: Long, run: () -> Unit) {
    if (USE_NOVA_SCHEDULER) AsyncExecutor.runLater(delay * 50, run)
    else Bukkit.getScheduler().runTaskLaterAsynchronously(NOVA_PLUGIN, run, delay)
}

fun runAsyncTaskSynchronized(lock: Any, run: () -> Unit) {
    val task = { synchronized(lock, run) }
    if (USE_NOVA_SCHEDULER) AsyncExecutor.run(task)
    else Bukkit.getScheduler().runTaskAsynchronously(NOVA_PLUGIN, task)
}

fun runAsyncTaskWithLock(lock: ObservableLock, run: () -> Unit) {
    val task = { lock.lockAndRun(run) }
    if (USE_NOVA_SCHEDULER) AsyncExecutor.run(task)
    else Bukkit.getScheduler().runTaskAsynchronously(NOVA_PLUGIN, task)
}

fun runAsyncTaskTimerSynchronized(lock: Any, delay: Long, period: Long, run: () -> Unit) =
    Bukkit.getScheduler().runTaskTimerAsynchronously(NOVA_PLUGIN, Runnable { synchronized(lock, run) }, delay, period)

fun runAsyncTaskTimer(delay: Long, period: Long, run: () -> Unit) =
    Bukkit.getScheduler().runTaskTimerAsynchronously(NOVA_PLUGIN, run, delay, period)

internal object AsyncExecutor {
    
    private val THREADS by MAIN_CONFIG.entry<Int>("performance", "nova_executor", "threads")
    
    private lateinit var threadFactory: ThreadFactory
    private lateinit var executorService: ScheduledExecutorService
    
    init {
        if (USE_NOVA_SCHEDULER) {
            threadFactory = ThreadFactoryBuilder().setNameFormat("Async Nova Worker - %d").build()
            executorService = ScheduledThreadPoolExecutor(THREADS, threadFactory)
            
            NOVA.disableHandlers += executorService::shutdown
        }
    }
    
    fun run(task: () -> Unit): Future<*> =
        executorService.submit {
            try {
                task()
            } catch (t: Throwable) {
                LOGGER.log(Level.SEVERE, "An exception occurred running a task", t)
            }
        }
    
    fun runLater(delay: Long, task: () -> Unit): Future<*> =
        executorService.schedule({
            try {
                task()
            } catch (t: Throwable) {
                LOGGER.log(Level.SEVERE, "An exception occurred running a task", t)
            }
        }, delay, TimeUnit.MILLISECONDS)
    
}
