package xyz.xenondevs.nova.util

import kotlinx.coroutines.SupervisorJob
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitScheduler
import xyz.xenondevs.nova.Nova

/**
 * Shortcut for [BukkitScheduler.runTask], registered under the Nova plugin.
 */
fun runTask(run: () -> Unit) =
    Bukkit.getScheduler().runTask(Nova, run)

/**
 * Shortcut for [BukkitScheduler.runTaskLater], registered under the Nova plugin.
 */
fun runTaskLater(delay: Long, run: () -> Unit) =
    Bukkit.getScheduler().runTaskLater(Nova, run, delay)

/**
 * Shortcut for [BukkitScheduler.runTaskTimer], registered under the Nova plugin.
 */
fun runTaskTimer(delay: Long, period: Long, run: () -> Unit) =
    Bukkit.getScheduler().runTaskTimer(Nova, run, delay, period)

/**
 * Shortcut for [BukkitScheduler.runTaskAsynchronously], registered under the Nova plugin.
 */
fun runAsyncTask(run: () -> Unit) =
    Bukkit.getScheduler().runTaskAsynchronously(Nova, run)

/**
 * Shortcut for [BukkitScheduler.runTaskLaterAsynchronously], registered under the Nova plugin.
 */
fun runAsyncTaskLater(delay: Long, run: () -> Unit) =
    Bukkit.getScheduler().runTaskLaterAsynchronously(Nova, run, delay)

/**
 * Shortcut for [BukkitScheduler.runTaskTimerAsynchronously], registered under the Nova plugin.
 */
fun runAsyncTaskTimer(delay: Long, period: Long, run: () -> Unit) =
    Bukkit.getScheduler().runTaskTimerAsynchronously(Nova, run, delay, period)

internal object AsyncExecutor {
    
    val SUPERVISOR = SupervisorJob()
    
}
