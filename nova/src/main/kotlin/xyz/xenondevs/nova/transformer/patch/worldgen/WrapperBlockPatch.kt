package xyz.xenondevs.nova.transformer.patch.worldgen

import net.minecraft.world.level.block.Block
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.LabelNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.util.insertAfterFirst
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.nova.transformer.MethodTransformer
import xyz.xenondevs.nova.util.findNthOfType
import xyz.xenondevs.nova.world.generation.wrapper.WrapperBlock

internal object WrapperBlockPatch : MethodTransformer(Block::class, "<init>", true) {
    
    override fun transform() {
        methodNode.insertAfterFirst(buildInsnList {
            val continueLabel = methodNode.instructions.findNthOfType<LabelNode>(1)
            aLoad(0)
            instanceOf(WrapperBlock::class.internalName)
            ifeq(continueLabel)
            addLabel()
            _return()
        }) { it.opcode == Opcodes.INVOKESPECIAL }
    }
    
}