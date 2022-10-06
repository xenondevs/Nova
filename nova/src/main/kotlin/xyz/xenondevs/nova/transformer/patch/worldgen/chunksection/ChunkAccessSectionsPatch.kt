package xyz.xenondevs.nova.transformer.patch.worldgen.chunksection

import net.minecraft.core.Registry
import net.minecraft.world.level.Level
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.chunk.LevelChunkSection
import org.eclipse.sisu.space.asm.Type
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.LabelNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.insertAfterFirst
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.bytebase.util.previous
import xyz.xenondevs.nova.transformer.MethodTransformer
import xyz.xenondevs.nova.util.ServerUtils
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.CHUNK_ACCESS_CONSTRUCTOR
import xyz.xenondevs.nova.util.reflection.defineClass

internal object ChunkAccessSectionsPatch : MethodTransformer(ChunkAccess::class, "<init>", Type.getConstructorDescriptor(CHUNK_ACCESS_CONSTRUCTOR), true) {
    
    override fun transform() {
        val wrapperClass = LevelChunkSectionWrapper::class
        if (!ServerUtils.isReload) {
            // Define wrapper class in the same class loader as the ChunkAccess class so there aren't any issues when reloading
            val sectionClass = LevelChunkSection::class.java
            sectionClass.classLoader.defineClass(wrapperClass.java.name, VirtualClassPath[wrapperClass].assemble(true), sectionClass.protectionDomain)
        }
        methodNode.insertAfterFirst(buildInsnList {
            
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
            getField(ChunkAccess::class.internalName, "SRF(net.minecraft.world.level.chunk.ChunkAccess sections)", "[LSRC/(net.minecraft.world.level.chunk.LevelChunkSection);")
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
            getField(ChunkAccess::class.internalName, "SRF(net.minecraft.world.level.chunk.ChunkAccess chunkPos)", "LSRC/(net.minecraft.world.level.ChunkPos);")
            aLoad(10) // sections
            iLoad(11) // i
            aaload()
            
            invokeSpecial(wrapperClass.internalName, "<init>", "(LSRC/(net.minecraft.world.level.Level);LSRC/(net.minecraft.world.level.ChunkPos);LSRC/(net.minecraft.world.level.chunk.LevelChunkSection);)V", false)
            aastore()
            
            add(incLabel)
            iinc(11, 1) // ++i
            goto(loopLabel)
            
        }) {
            it.opcode == Opcodes.PUTFIELD && (it as FieldInsnNode).desc == Type.getDescriptor(Registry::class.java)
        }
    }
    
    
}