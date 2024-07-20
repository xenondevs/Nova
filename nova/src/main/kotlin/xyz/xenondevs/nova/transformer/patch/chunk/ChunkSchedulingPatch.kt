package xyz.xenondevs.nova.transformer.patch.chunk

import ca.spottedleaf.concurrentutil.executor.standard.PrioritisedExecutor
import ca.spottedleaf.moonrise.common.util.ChunkSystem
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.status.ChunkStatus
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.format.WorldDataManager
import java.util.function.Consumer
import kotlin.reflect.KFunction

private val CHUNK_TASK_SCHEDULER_SCHEDULE_CHUNK_LOAD_METHOD = ReflectionUtils.getMethod(
    ChunkTaskScheduler::class,
    false,
    "scheduleChunkLoad",
    Int::class, Int::class, ChunkStatus::class, Boolean::class, PrioritisedExecutor.Priority::class, Consumer::class
)

internal object ChunkSchedulingPatch : MultiTransformer(ChunkTaskScheduler::class, ChunkSystem::class) {
    
    override fun transform() {
        VirtualClassPath[CHUNK_TASK_SCHEDULER_SCHEDULE_CHUNK_LOAD_METHOD]
            .instructions.insert(buildInsnList {
                addLabel()
                aLoad(0)
                iLoad(1)
                iLoad(2)
                invokeStatic(::handleScheduleChunkLoad)
            })
        
        fun insertEnableTicking(fn: KFunction<*>) =
            VirtualClassPath[fn].instructions.insert(buildInsnList {
                addLabel()
                aLoad(0)
                invokeStatic(::enableChunkTicking)
            })
        
        fun insertDisableTicking(fn: KFunction<*>) =
            VirtualClassPath[fn].instructions.insert(buildInsnList {
                addLabel()
                aLoad(0)
                invokeStatic(::disableChunkTicking)
            })
        
        insertEnableTicking(ChunkSystem::onChunkEntityTicking)
        insertEnableTicking(ChunkSystem::onChunkTicking)
        insertDisableTicking(ChunkSystem::onChunkNotTicking)
        insertDisableTicking(ChunkSystem::onChunkNotEntityTicking)
    }
    
    @JvmStatic
    fun handleScheduleChunkLoad(scheduler: ChunkTaskScheduler, x: Int, z: Int) {
        WorldDataManager.loadAsync(ChunkPos(scheduler.world.uuid, x, z))
    }
    
    @JvmStatic
    fun enableChunkTicking(chunk: LevelChunk) {
        WorldDataManager.startTicking(ChunkPos(chunk.level.uuid, chunk.locX, chunk.locZ))
    }
    
    @JvmStatic
    fun disableChunkTicking(chunk: LevelChunk) {
        WorldDataManager.stopTicking(ChunkPos(chunk.level.uuid, chunk.locX, chunk.locZ))
    }
    
}