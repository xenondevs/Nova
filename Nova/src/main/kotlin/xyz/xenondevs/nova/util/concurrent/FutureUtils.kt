package xyz.xenondevs.nova.util.concurrent

import xyz.xenondevs.nova.util.minecraftServer
import xyz.xenondevs.nova.util.runTask
import java.util.concurrent.CompletableFuture

fun CompletableFuture<Boolean>.runIfTrue(run: () -> Unit) {
    val mainThread = minecraftServer.serverThread == Thread.currentThread()
    thenRun {
        if (get()) {
            if (mainThread && minecraftServer.serverThread != Thread.currentThread()) 
                runTask(run)
            else run()
        }
    }
}

fun CompletableFuture<Boolean>.runIfTrueSynchronized(lock: Any, run: () -> Unit) {
    runIfTrue { synchronized(lock, run) }
}