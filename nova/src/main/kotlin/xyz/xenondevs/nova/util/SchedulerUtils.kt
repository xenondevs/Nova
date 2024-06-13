package xyz.xenondevs.nova.util

import kotlinx.coroutines.SupervisorJob
import org.bukkit.Bukkit
import xyz.xenondevs.nova.NOVA_PLUGIN

fun runTaskLater(delay: Long, run: () -> Unit) =
    Bukkit.getScheduler().runTaskLater(NOVA_PLUGIN, run, delay)

fun runTask(run: () -> Unit) =
    Bukkit.getScheduler().runTask(NOVA_PLUGIN, run)

fun runTaskTimer(delay: Long, period: Long, run: () -> Unit) =
    Bukkit.getScheduler().runTaskTimer(NOVA_PLUGIN, run, delay, period)

fun runAsyncTask(run: () -> Unit) =
    Bukkit.getScheduler().runTaskAsynchronously(NOVA_PLUGIN, run)

fun runAsyncTaskLater(delay: Long, run: () -> Unit) =
    Bukkit.getScheduler().runTaskLaterAsynchronously(NOVA_PLUGIN, run, delay)

fun runAsyncTaskTimer(delay: Long, period: Long, run: () -> Unit) =
    Bukkit.getScheduler().runTaskTimerAsynchronously(NOVA_PLUGIN, run, delay, period)

internal object AsyncExecutor {
    
    val SUPERVISOR = SupervisorJob()

}
