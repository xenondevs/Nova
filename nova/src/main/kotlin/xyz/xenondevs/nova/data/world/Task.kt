package xyz.xenondevs.nova.data.world

import org.bukkit.World
import xyz.xenondevs.nova.world.ChunkPos
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal sealed interface Task

internal sealed class ChunkTask(val pos: ChunkPos) : Task {
    
    private val latch = CountDownLatch(1)
    
    fun markCompleted() {
        latch.countDown()
    }
    
    fun awaitCompletion() {
        latch.await()
    }
    
    fun awaitCompletion(timeout: Long, unit: TimeUnit): Boolean {
        return latch.await(timeout, unit)
    }
    
}

internal sealed class WorldTask(val world: World) : Task

internal class ChunkLoadTask(pos: ChunkPos) : ChunkTask(pos) {
    override fun toString() = "ChunkLoadTask($pos)"
}

internal class ChunkUnloadTask(pos: ChunkPos) : ChunkTask(pos) {
    override fun toString() = "ChunkUnloadTask($pos)"
}

internal class SaveWorldTask(world: World) : WorldTask(world) {
    override fun toString() = "SaveWorldTask($world)"
}