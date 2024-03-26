package xyz.xenondevs.nova.util.concurrent

import xyz.xenondevs.nova.util.MINECRAFT_SERVER
import xyz.xenondevs.nova.util.runTask

val isServerThread: Boolean
    get() = Thread.currentThread() == MINECRAFT_SERVER.serverThread

fun runInServerThread(task: () -> Unit) {
    if (isServerThread) task()
    else runTask(task)
}

fun checkServerThread() {
    check(isServerThread) { "Not on server thread" }
}