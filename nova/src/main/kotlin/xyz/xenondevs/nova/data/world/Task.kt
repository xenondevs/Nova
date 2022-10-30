package xyz.xenondevs.nova.data.world

import org.bukkit.World
import xyz.xenondevs.nova.world.ChunkPos
import java.util.concurrent.CountDownLatch

internal interface Task

internal open class ChunkTask(val pos: ChunkPos) : Task {
    
    private val latch = CountDownLatch(1)
    
    fun markCompleted() {
        latch.countDown()
    }
    
    fun awaitCompletion() {
        latch.await()
    }
    
}

internal class ChunkLoadTask(pos: ChunkPos) : ChunkTask(pos)

internal class ChunkUnloadTask(pos: ChunkPos) : ChunkTask(pos)

internal open class WorldTask(val world: World) : Task

internal class SaveWorldTask(world: World) : WorldTask(world)