package xyz.xenondevs.nova.util

import kotlinx.coroutines.SupervisorJob
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitScheduler
import org.bukkit.scheduler.BukkitTask
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.PLUGIN_READY

/**
 * Shortcut for [BukkitScheduler.runTask], registered under the Nova plugin.
 */
fun runTask(run: () -> Unit): BukkitTask {
    checkSchedulerAvailability()
    return Bukkit.getScheduler().runTask(Nova, run)
}

/**
 * Shortcut for [BukkitScheduler.runTaskLater], registered under the Nova plugin.
 */
fun runTaskLater(delay: Long, run: () -> Unit): BukkitTask {
    checkSchedulerAvailability()
    return Bukkit.getScheduler().runTaskLater(Nova, run, delay)
}

/**
 * Shortcut for [BukkitScheduler.runTaskTimer], registered under the Nova plugin.
 */
fun runTaskTimer(delay: Long, period: Long, run: () -> Unit): BukkitTask {
    checkSchedulerAvailability()
    return Bukkit.getScheduler().runTaskTimer(Nova, run, delay, period)
}

/**
 * Shortcut for [BukkitScheduler.runTaskAsynchronously], registered under the Nova plugin.
 */
fun runAsyncTask(run: () -> Unit): BukkitTask {
    checkSchedulerAvailability()
    return Bukkit.getScheduler().runTaskAsynchronously(Nova, run)
}

/**
 * Shortcut for [BukkitScheduler.runTaskLaterAsynchronously], registered under the Nova plugin.
 */
fun runAsyncTaskLater(delay: Long, run: () -> Unit): BukkitTask {
    checkSchedulerAvailability()
    return Bukkit.getScheduler().runTaskLaterAsynchronously(Nova, run, delay)
}

/**
 * Shortcut for [BukkitScheduler.runTaskTimerAsynchronously], registered under the Nova plugin.
 */
fun runAsyncTaskTimer(delay: Long, period: Long, run: () -> Unit): BukkitTask {
    checkSchedulerAvailability()
    return Bukkit.getScheduler().runTaskTimerAsynchronously(Nova, run, delay, period)
}

private fun checkSchedulerAvailability() {
    check(PLUGIN_READY) { "Scheduler cannot be used this early! Use a post-world initialization stage for this." }
}

internal object AsyncExecutor {
    
    val SUPERVISOR = SupervisorJob()
    
}
