package xyz.xenondevs.nova.transformer.patch.worldgen.chunksection

import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelHeightAccessor
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.chunk.LevelChunkSection
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.LabelNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.util.insertAfterFirst
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.bytebase.util.previous
import xyz.xenondevs.bytebase.util.puts
import xyz.xenondevs.nova.transformer.MethodTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.CHUNK_ACCESS_CONSTRUCTOR
import xyz.xenondevs.nova.util.reflection.ReflectionUtils

private val CHUNK_ACCESS_MAX_SECTION = ReflectionUtils.getField(ChunkAccess::class, "maxSection")

/**
 * Patch to replace all [LevelChunkSection]s with Nova's [LevelChunkSectionWrapper].
 */
internal object ChunkAccessSectionsPatch : MethodTransformer(CHUNK_ACCESS_CONSTRUCTOR, true) {
    
    override fun transform() {
        val wrapperClass = LevelChunkSectionWrapper::class
        methodNode.insertAfterFirst(buildInsnList {
            // Just a for loop to replace all sections.
            
            val returnLabel = methodNode.instructions.last.previous(3) as LabelNode
            val loopLabel = LabelNode()
            val incLabel = LabelNode()
            
            addLabel()
            aLoad(3)
            instanceOf(Level::class.internalName)
            ifeq(returnLabel)
            
            addLabel()
            aLoad(3)
            checkCast(Level::class.internalName)
            aStore(9) // level
            aLoad(0)
            getField(ChunkAccess::class.internalName, "sections", "[Lnet/minecraft/world/level/chunk/LevelChunkSection;")
            aStore(10) // sections
            ldc(0)
            iStore(11) // i
            
            add(loopLabel)
            iLoad(11) // i
            aLoad(10) // sections
            arraylength()
            if_icmpge(returnLabel)
            
            addLabel()
            
            aLoad(10) // sections
            iLoad(11) // i
            
            new(wrapperClass)
            dup()
            
            aLoad(9) // level
            aLoad(0)
            getField(ChunkAccess::class.internalName, "chunkPos", "Lnet/minecraft/world/level/ChunkPos;")
            iLoad(11) // i
            aLoad(3) // LevelHeightAccessor
            invokeInterface(LevelHeightAccessor::getMinSection)
            iadd()
            ldc(4)
            ishl()
            aLoad(10) // sections
            iLoad(11) // i
            aaload()
            
            invokeSpecial(::LevelChunkSectionWrapper)
            aastore()
            
            add(incLabel)
            iinc(11, 1) // ++i
            goto(loopLabel)
            
        }) {
            it.opcode == Opcodes.PUTFIELD && (it as FieldInsnNode).puts(CHUNK_ACCESS_MAX_SECTION) // last insn
        }
    }
    
    
}