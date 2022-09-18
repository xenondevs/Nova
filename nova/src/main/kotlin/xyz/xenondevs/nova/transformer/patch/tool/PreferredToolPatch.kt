package xyz.xenondevs.nova.transformer.patch.tool

import net.minecraft.world.level.block.state.BlockState
import org.bukkit.craftbukkit.v1_19_R1.block.CraftBlock
import org.objectweb.asm.Type
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.nova.transformer.MethodTransformer
import xyz.xenondevs.nova.util.bukkitStack
import xyz.xenondevs.nova.util.item.ToolUtils
import xyz.xenondevs.nova.util.item.takeUnlessAir
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import kotlin.reflect.jvm.jvmName
import net.minecraft.world.item.ItemStack as MojangStack

internal object PreferredToolPatch : MethodTransformer(ReflectionRegistry.CRAFT_BLOCK_IS_PREFERRED_TOOL_METHOD, true) {
    
    override fun transform() {
        val newMethod = ReflectionUtils.getMethod(PreferredToolPatch::class.java, false, "isPreferredTool", CraftBlock::class.java, BlockState::class.java, MojangStack::class.java)
        
        methodNode.instructions = buildInsnList {
            aLoad(0)
            aLoad(1)
            aLoad(2)
            invokeStatic(
                PreferredToolPatch::class.jvmName.replace('.', '/'),
                newMethod.name,
                Type.getMethodDescriptor(newMethod)
            )
            ireturn()
        }
    }
    
    @JvmStatic
    fun isPreferredTool(block: CraftBlock, blockState: BlockState, tool: MojangStack): Boolean {
        return !blockState.requiresCorrectToolForDrops() || ToolUtils.isCorrectToolForDrops(tool.bukkitStack.takeUnlessAir(), block)
    }
    
}