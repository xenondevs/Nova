package xyz.xenondevs.nova.patch.adapter

import net.minecraft.world.level.chunk.LevelChunkSection
import org.objectweb.asm.tree.MethodNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.nova.util.data.AsmUtils

object LcsWrapperAdapter : Adapter {
    
    private val COPY_BLOCK_COUNTS_METHODS: Set<String> = hashSetOf("updateKnownBlockInfo")
    
    override fun adapt(clazz: ClassWrapper) {
        val methods = AsmUtils.listNonOverriddenMethods(clazz, VirtualClassPath[LevelChunkSection::class])
        
        for (method in methods) {
            val delegatingMethod = MethodNode(method.access, method.name, method.desc, null, null)
            delegatingMethod.instructions = AsmUtils.createDelegateInstructions(
                buildInsnList {
                    aLoad(0)
                    getField(clazz.name, "delegate", "L" + LevelChunkSection::class.internalName + ";")
                },
                buildInsnList {
                    invokeVirtual(LevelChunkSection::class.internalName, method.name, method.desc)
                    if (method.name in COPY_BLOCK_COUNTS_METHODS) {
                        aLoad(0)
                        invokeVirtual(clazz.name, "copyBlockCounts", "()V")
                    }
                },
                method
            )
            clazz.methods.add(delegatingMethod)
        }
    }
    
}