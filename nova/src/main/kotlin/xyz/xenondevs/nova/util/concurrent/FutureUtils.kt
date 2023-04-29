package xyz.xenondevs.nova.util.concurrent

import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.util.MINECRAFT_SERVER
import xyz.xenondevs.nova.util.runTask
import java.util.concurrent.CompletableFuture
import java.util.logging.Level

fun CompletableFuture<Boolean>.runIfTrueOnSimilarThread(run: () -> Unit) {
    val mainThread = MINECRAFT_SERVER.serverThread == Thread.currentThread()
    thenApply {
        try {
            if (it) {
                if (mainThread && MINECRAFT_SERVER.serverThread != Thread.currentThread())
                    runTask(run)
                else run()
            }
        } catch (t: Throwable) {
            LOGGER.log(Level.SEVERE, "", t)
        }
    }
}

fun CompletableFuture<Boolean>.runIfTrue(run: () -> Unit) {
    thenApply {
        try {
            if (it) run()
        } catch (t: Throwable) {
            LOGGER.log(Level.SEVERE, "", t)
        }
    }
}

fun <T> CompletableFuture<T>.completeServerThread(supplier: () -> T) {
    if (MINECRAFT_SERVER.serverThread == Thread.currentThread())
        complete(supplier())
    else runTask { complete(supplier()) }
}

fun <K, V> Map<K, V>.mapToAllFuture(transform: (Map.Entry<K, V>) -> CompletableFuture<*>?): CompletableFuture<Void> =
    CompletableFuture.allOf(*mapNotNull(transform).toTypedArray())

fun <T> Iterable<T>.mapToAllFuture(transform: (T) -> CompletableFuture<*>?): CompletableFuture<Void> =
    CompletableFuture.allOf(*mapNotNull(transform).toTypedArray())

fun <T> Array<T>.mapToAllFuture(transform: (T) -> CompletableFuture<*>?): CompletableFuture<Void> =
    CompletableFuture.allOf(*mapNotNull(transform).toTypedArray())