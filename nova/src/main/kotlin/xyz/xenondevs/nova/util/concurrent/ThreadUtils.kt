package xyz.xenondevs.nova.util.concurrent

import org.spigotmc.WatchdogThread
import xyz.xenondevs.nova.util.MINECRAFT_SERVER

internal val isServerThread: Boolean
    get() = Thread.currentThread() == MINECRAFT_SERVER.runningThread || Thread.currentThread() is WatchdogThread

internal fun checkServerThread() {
    check(isServerThread) { "Not on server thread" }
}