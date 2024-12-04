package xyz.xenondevs.nova.patch.impl.block

import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.state.BlockState
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodInsnNode
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.replaceEvery
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.patch.MultiTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.util.toNovaPos
import xyz.xenondevs.nova.world.block.logic.place.BlockPlacing

private val BLOCK_ITEM_PLACE_BLOCK = ReflectionUtils.getMethod(BlockItem::class, "placeBlock", BlockPlaceContext::class, BlockState::class)

internal object EarlyBlockPlaceEventPatch : MultiTransformer(BlockItem::class) {
    
    override fun transform() {
        VirtualClassPath[BlockItem::place].replaceEvery(
            0, 0,
            { invokeStatic(::placeBlock) }
        ) { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(BLOCK_ITEM_PLACE_BLOCK) }
    }
    
    @JvmStatic
    fun placeBlock(blockItem: BlockItem, context: BlockPlaceContext, state: BlockState): Boolean {
        try {
            val pos = context.clickedPos.toNovaPos(context.level.world)
            if (!BlockPlacing.handleBlockPlace(pos)) {
                context.player?.containerMenu?.sendAllDataToRemote()
                return false
            }
        } catch (t: Throwable) {
            LOGGER.error("An exception occurred while handling early block place event", t)
        }
        
        return BLOCK_ITEM_PLACE_BLOCK.invoke(blockItem, context, state) as Boolean
    }
    
}