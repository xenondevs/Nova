package xyz.xenondevs.nova.transformer.patch.block

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.chunk.LevelChunk
import org.objectweb.asm.Opcodes
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.replaceEvery
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.transformer.patch.worldgen.chunksection.LevelChunkSectionWrapper
import xyz.xenondevs.nova.util.chunkSection
import xyz.xenondevs.nova.util.toNovaPos
import xyz.xenondevs.nova.world.block.migrator.BlockMigrator

internal object BlockMigrationPatches : MultiTransformer(Level::class, LevelChunk::class) {
    
    override fun transform() {
        transformLevelChunkSetBlockEntity()
        transformLevelNotifyAndUpdatePhysics()
    }
    
    private fun transformLevelChunkSetBlockEntity() {
        VirtualClassPath[LevelChunk::addAndRegisterBlockEntity].instructions.replaceEvery(0, 0, {
            aLoad(1)
            invokeStatic(::handleBlockEntityPlaced)
            _return()
        }) { it.opcode == Opcodes.RETURN }
        
        VirtualClassPath[LevelChunk::removeBlockEntity].instructions.insert(buildInsnList { 
            addLabel()
            aLoad(0)
            aLoad(1)
            invokeStatic(::handleBlockEntityRemoved)
        })
    }
    
    @JvmStatic
    fun handleBlockEntityPlaced(blockEntity: BlockEntity) {
        val novaPos = blockEntity.blockPos.toNovaPos(blockEntity.level!!.world)
        if ((novaPos.chunkSection as LevelChunkSectionWrapper).isMigrationActive) {
            BlockMigrator.handleBlockEntityPlaced(blockEntity.blockPos.toNovaPos(blockEntity.level!!.world), blockEntity)
        }
    }
    
    @JvmStatic
    fun handleBlockEntityRemoved(chunk: LevelChunk, pos: BlockPos) {
        val novaPos = pos.toNovaPos(chunk.level.world)
        if ((novaPos.chunkSection as LevelChunkSectionWrapper).isMigrationActive) {
            BlockMigrator.handleBlockEntityPlaced(pos.toNovaPos(chunk.level.world), null)
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