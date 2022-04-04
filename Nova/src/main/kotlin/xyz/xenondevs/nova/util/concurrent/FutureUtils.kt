package xyz.xenondevs.nova.util.concurrent

import xyz.xenondevs.nova.util.minecraftServer
import xyz.xenondevs.nova.util.runTask
import java.util.concurrent.CompletableFuture

fun CompletableFuture<Boolean>.runIfTrue(run: () -> Unit) {
    val mainThread = minecraftServer.serverThread == Thread.currentThread()
    thenRun {
        try {
            if (get()) {
                if (mainThread && minecraftServer.serverThread != Thread.currentThread())
                    runTask(run)
                else run()
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}

fun CompletableFuture<Boolean>.runIfTrueSynchronized(lock: Any, run: () -> Unit) {
    runIfTrue { synchronized(lock, run) }
}

fun <K, V> Map<K, V>.mapToAllFuture(transform: (Map.Entry<K, V>) -> CompletableFuture<*>?): CompletableFuture<Void> =
    CompletableFuture.allOf(*mapNotNull(transform).toTypedArray())

fun <T> Iterable<T>.mapToAllFuture(transform: (T) -> CompletableFuture<*>?): CompletableFuture<Void> =
    CompletableFuture.allOf(*mapNotNull(transform).toTypedArray())

fun <T> Array<T>.mapToAllFuture(transform: (T) -> CompletableFuture<*>?): CompletableFuture<Void> =
    CompletableFuture.allOf(*mapNotNull(transform).toTypedArray())