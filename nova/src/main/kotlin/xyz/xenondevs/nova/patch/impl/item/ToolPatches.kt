package xyz.xenondevs.nova.patch.impl.item

import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.craftbukkit.block.CraftBlock
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.gets
import xyz.xenondevs.bytebase.util.replaceEvery
import xyz.xenondevs.nova.patch.MultiTransformer
import xyz.xenondevs.nova.util.item.ToolUtils
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.world.item.behavior.Tool

private val ITEM_STACK_IS_TAG = ReflectionUtils.getMethod(ItemStack::class, "is", TagKey::class)

@Suppress("unused")
internal object ToolPatches : MultiTransformer(CraftBlock::class, Player::class, ItemStack::class) {
    
    override fun transform() {
        transformCraftBlockIsPreferredTool()
        transformPlayerAttack()
    }
    
    /**
     * Patches the CraftBlock#isPreferredTool method to properly handle Nova's tools.
     */
    private fun transformCraftBlockIsPreferredTool() {
        VirtualClassPath[ReflectionRegistry.CRAFT_BLOCK_DATA_IS_PREFERRED_TOOL_METHOD].replaceInstructions {
            aLoad(0)
            aLoad(1)
            aLoad(2)
            invokeStatic(::isPreferredTool)
            ireturn()
        }
    }
    
    @JvmStatic
    fun isPreferredTool(block: CraftBlock, blockState: BlockState, tool: ItemStack): Boolean {
        return !blockState.requiresCorrectToolForDrops() || ToolUtils.isCorrectToolForDrops(block, tool.asBukkitMirror().takeUnlessEmpty())
    }
    
    /**
     * Patches the Player#attack method to use properly perform sweep attacks.
     */
    private fun transformPlayerAttack() {
        VirtualClassPath[Player::attack].replaceEvery(
            0, 1,
            { invokeStatic(::canDoSweepAttack) },
            {
                it.opcode == Opcodes.GETSTATIC && (it as FieldInsnNode).gets(ItemTags::SWORDS)
                    && it.next.opcode == Opcodes.INVOKEVIRTUAL && (it.next as MethodInsnNode).calls(ITEM_STACK_IS_TAG)
            }
        )
    }
    
    @JvmStatic
    fun canDoSweepAttack(itemStack: ItemStack): Boolean {
        val novaItem = itemStack.novaItem
        
        if (novaItem != null) {
            return novaItem.getBehaviorOrNull<Tool>()?.canSweepAttack == true
        } else {
            return itemStack.`is`(ItemTags.SWORDS)
        }
    }
    
}