package xyz.xenondevs.nova.transformer.patch.chunk

import ca.spottedleaf.concurrentutil.executor.standard.PrioritisedExecutor
import io.papermc.paper.chunk.system.scheduling.ChunkTaskScheduler
import net.minecraft.world.level.chunk.ChunkStatus
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.nova.transformer.MethodTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.format.WorldDataManager
import java.util.function.Consumer

private val CHUNK_TASK_SCHEDULER_SCHEDULE_CHUNK_LOAD_METHOD = ReflectionUtils.getMethod(
    ChunkTaskScheduler::class,
    false,
    "scheduleChunkLoad",
    Int::class, Int::class, ChunkStatus::class, Boolean::class, PrioritisedExecutor.Priority::class, Consumer::class
)

internal object ChunkLoadSchedulingPatch : MethodTransformer(CHUNK_TASK_SCHEDULER_SCHEDULE_CHUNK_LOAD_METHOD) {
    
    override fun transform() {
        methodNode.instructions.insert(buildInsnList { 
            addLabel()
            aLoad(0)
            iLoad(1)
            iLoad(2)
            invokeStatic(::handleScheduleChunkLoad)
        })
    }
    
    @JvmStatic
    fun handleScheduleChunkLoad(scheduler: ChunkTaskScheduler, x: Int, z: Int) {
        WorldDataManager.loadAsync(ChunkPos(scheduler.world.uuid, x, z))
    }
    
}