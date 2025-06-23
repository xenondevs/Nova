package xyz.xenondevs.nova.world.format

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.xenondevs.nova.LOGGER
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.moveTo
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

private class SerializedFile(val path: Path, val bin: ByteArray) {
    operator fun component1(): Path = path
    operator fun component2(): ByteArray = bin
}

internal class AsyncFileAccess {
    
    private val channel = Channel<SerializedFile>(capacity = Channel.UNLIMITED)
    private val pendingWrites = ConcurrentHashMap<Path, ByteArray?>()
    
    private val job = CoroutineScope(Dispatchers.IO).launch(CoroutineName("Nova AFA")) {
        channel.consumeEach { (path, bin) ->
            try {
                val tmpPath = path.resolveSibling(path.name + ".tmp")
                tmpPath.writeBytes(bin)
                tmpPath.moveTo(path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                pendingWrites.computeIfPresent(path) { _, pendingBin -> if (pendingBin === bin) null else pendingBin }
            } catch (t: Throwable) {
                LOGGER.error("Failed to save file: $path", t)
            }
        }
    }
    
    /**
     * Schedules writing [bin] to [path].
     */
    suspend fun write(path: Path, bin: ByteArray) {
        pendingWrites[path] = bin
        channel.send(SerializedFile(path, bin))
    }
    
    /**
     * Reads bytes from [path] (using pending bytes if present) or returns `null` if the file does not exist.
     */
    suspend fun read(path: Path): ByteArray? {
        val pendingBin = pendingWrites[path]
        if (pendingBin != null)
            return pendingBin
        
        return withContext(Dispatchers.IO) { 
           if (path.exists() && path.fileSize() > 0L) path.readBytes() else null
        }
    }
    
    /**
     * Shuts down [AsyncFileAccess] and waits for all pending writes to be completed.
     */
    suspend fun shutdownAndWait() {
        channel.close()
        job.join()
    }
    
}