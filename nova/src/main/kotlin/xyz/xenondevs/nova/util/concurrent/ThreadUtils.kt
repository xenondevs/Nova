package xyz.xenondevs.nova.util.concurrent

import xyz.xenondevs.nova.util.minecraftServer
import xyz.xenondevs.nova.util.runTask

val isServerThread: Boolean
    get() = Thread.currentThread() == minecraftServer.serverThread

fun runInServerThread(task: () -> Unit) {
    if (isServerThread) task()
    else runTask(task)
}