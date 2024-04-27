package xyz.xenondevs.nova.util.concurrent

import xyz.xenondevs.nova.util.MINECRAFT_SERVER

internal val isServerThread: Boolean
    get() = Thread.currentThread() == MINECRAFT_SERVER.serverThread

internal fun checkServerThread() {
    check(isServerThread) { "Not on server thread" }
}