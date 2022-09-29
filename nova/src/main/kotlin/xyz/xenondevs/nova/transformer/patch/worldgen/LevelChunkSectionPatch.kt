package xyz.xenondevs.nova.transformer.patch.worldgen

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import org.objectweb.asm.tree.LabelNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.nova.transformer.MethodTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.world.generation.wrapper.WrapperBlockState

internal object LevelChunkSectionPatch : MethodTransformer(ReflectionRegistry.LEVEL_CHUNK_SECTION_SET_BLOCK_STATE_METHOD, true) {
    
    private val WRAPPER_INTERNAL_NAME = WrapperBlockState::class.internalName
    
    override fun transform() {
        val instructions = methodNode.instructions
        val startLabel = instructions[0] as LabelNode
        instructions.insert(buildInsnList {
            addLabel()
            aLoad(4)
            instanceOf(WRAPPER_INTERNAL_NAME)
            ifeq(startLabel)
            addLabel()
            getStatic(Blocks::class.internalName, "AIR", "LSRC/(net.minecraft.world.level.block.Block);")
            invokeVirtual(Block::class.internalName, "SRM(net.minecraft.world.level.block.Block defaultBlockState)", "()LSRC/(net.minecraft.world.level.block.state.BlockState);")
            areturn()
        })
    }
    
}