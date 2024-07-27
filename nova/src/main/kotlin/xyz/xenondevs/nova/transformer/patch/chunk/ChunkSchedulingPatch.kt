package xyz.xenondevs.nova.transformer.patch.chunk

import ca.spottedleaf.moonrise.common.util.ChunkSystem
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.task.GenericDataLoadTask
import kotlinx.coroutines.runBlocking
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.chunk.LevelChunk
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.format.WorldDataManager
import kotlin.reflect.KFunction


private val CHUNK_DATA_LOAD_TASK = Class.forName("ca.spottedleaf.moonrise.patches.chunk_system.scheduling.task.ChunkLoadTask\$ChunkDataLoadTask")
private val CHUNK_DATA_LOAD_TASK_RUN_OFF_MAIN = ReflectionUtils.getMethod(
    CHUNK_DATA_LOAD_TASK,
    "runOffMain",
    CompoundTag::class, Throwable::class
)

private val GENERIC_DATA_LOAD_TASK_WORLD = ReflectionUtils.getField(GenericDataLoadTask::class, "world")
private val GENERIC_DATA_LOAD_TASK_CHUNK_X = ReflectionUtils.getField(GenericDataLoadTask::class, "chunkX")
private val GENERIC_DATA_LOAD_TASK_CHUNK_Z = ReflectionUtils.getField(GenericDataLoadTask::class, "chunkZ")

internal object ChunkSchedulingPatch : MultiTransformer(CHUNK_DATA_LOAD_TASK.kotlin, ChunkSystem::class) {
    
    override fun transform() {
        VirtualClassPath[CHUNK_DATA_LOAD_TASK_RUN_OFF_MAIN].instructions.insert(buildInsnList {
            addLabel()
            aLoad(0)
            getField(GENERIC_DATA_LOAD_TASK_WORLD)
            aLoad(0)
            getField(GENERIC_DATA_LOAD_TASK_CHUNK_X)
            aLoad(0)
            getField(GENERIC_DATA_LOAD_TASK_CHUNK_Z)
            invokeStatic(::loadChunkBlocking)
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
    fun loadChunkBlocking(level: ServerLevel, chunkX: Int, chunkZ: Int) {
        runBlocking { WorldDataManager.getOrLoadChunk(ChunkPos(level.world.uid, chunkX, chunkZ)) }
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