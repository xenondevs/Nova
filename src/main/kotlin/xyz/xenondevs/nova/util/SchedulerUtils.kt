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
