package xyz.xenondevs.nova.patch.impl.block

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.replaceEvery
import xyz.xenondevs.nova.world.isMigrationActive
import xyz.xenondevs.nova.patch.MultiTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.util.toNovaPos
import xyz.xenondevs.nova.world.block.migrator.BlockMigrator

private val LEVEL_CHUNK_SET_BLOCK_STATE = ReflectionUtils.getMethod(
    LevelChunk::class,
    "setBlockState",
    BlockPos::class, BlockState::class, Int::class
)

internal object BlockMigrationPatches : MultiTransformer(Level::class, LevelChunk::class) {
    
    override fun transform() {
        transformLevelChunkSetBlockState()
        transformLevelNotifyAndUpdatePhysics()
    }
    
    private fun transformLevelChunkSetBlockState() {
        VirtualClassPath[LEVEL_CHUNK_SET_BLOCK_STATE].instructions.replaceEvery(
            0, 0,
            { // on stack: this, blockEntity
                dup2()
                invokeVirtual(LevelChunk::addAndRegisterBlockEntity)
                invokeStatic(BlockMigrationPatches::handleBlockEntityPlaced)
            }
        ) { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(LevelChunk::addAndRegisterBlockEntity) }
        
        VirtualClassPath[LEVEL_CHUNK_SET_BLOCK_STATE].instructions.replaceEvery(
            0, 0,
            { // on stack: this, pos
                dup2()
                invokeVirtual(LevelChunk::removeBlockEntity)
                invokeStatic(BlockMigrationPatches::handleBlockEntityRemoved)
            }
        ) { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(LevelChunk::removeBlockEntity) }
    }
    
    @JvmStatic
    fun handleBlockEntityPlaced(chunk: LevelChunk, blockEntity: BlockEntity) {
        val chunkSection = chunk.getSection(chunk.getSectionIndex(blockEntity.blockPos.y))
        if (chunkSection.isMigrationActive) {
            val novaPos = blockEntity.blockPos.toNovaPos(chunk.level.world)
            BlockMigrator.handleBlockEntityPlaced(novaPos, blockEntity)
        }
    }
    
    @JvmStatic
    fun handleBlockEntityRemoved(chunk: LevelChunk, pos: BlockPos) {
        val chunkSection = chunk.getSection(chunk.getSectionIndex(pos.y))
        if (chunkSection.isMigrationActive) {
            val novaPos = pos.toNovaPos(chunk.level.world)
            BlockMigrator.handleBlockEntityPlaced(novaPos, null)
        }
    }
    
    private fun transformLevelNotifyAndUpdatePhysics() {
        // I don't know why this the newBlock == actualBlock check exists, but this
        // patch is necessary to prevent desync caused by block migration. Let's hope this doesn't blow up.
        VirtualClassPath[Level::notifyAndUpdatePhysics].instructions.insert(buildInsnList {
            // newBlock = actualBlock;
            addLabel()
            aLoad(5)
            aStore(4)
        })
    }
    
}