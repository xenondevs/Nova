package xyz.xenondevs.nova.util

import kotlinx.coroutines.SupervisorJob
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitScheduler
import xyz.xenondevs.nova.NOVA

/**
 * Shortcut for [BukkitScheduler.runTask], registered under the Nova plugin.
 */
fun runTask(run: () -> Unit) =
    Bukkit.getScheduler().runTask(NOVA, run)

/**
 * Shortcut for [BukkitScheduler.runTaskLater], registered under the Nova plugin.
 */
fun runTaskLater(delay: Long, run: () -> Unit) =
    Bukkit.getScheduler().runTaskLater(NOVA, run, delay)

/**
 * Shortcut for [BukkitScheduler.runTaskTimer], registered under the Nova plugin.
 */
fun runTaskTimer(delay: Long, period: Long, run: () -> Unit) =
    Bukkit.getScheduler().runTaskTimer(NOVA, run, delay, period)

/**
 * Shortcut for [BukkitScheduler.runTaskAsynchronously], registered under the Nova plugin.
 */
fun runAsyncTask(run: () -> Unit) =
    Bukkit.getScheduler().runTaskAsynchronously(NOVA, run)

/**
 * Shortcut for [BukkitScheduler.runTaskLaterAsynchronously], registered under the Nova plugin.
 */
fun runAsyncTaskLater(delay: Long, run: () -> Unit) =
    Bukkit.getScheduler().runTaskLaterAsynchronously(NOVA, run, delay)

/**
 * Shortcut for [BukkitScheduler.runTaskTimerAsynchronously], registered under the Nova plugin.
 */
fun runAsyncTaskTimer(delay: Long, period: Long, run: () -> Unit) =
    Bukkit.getScheduler().runTaskTimerAsynchronously(NOVA, run, delay, period)

internal object AsyncExecutor {
    
    val SUPERVISOR = SupervisorJob()
    
}
