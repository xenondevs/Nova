package xyz.xenondevs.nova.transformer.patch.item

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.SwordItem
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.craftbukkit.v1_20_R1.block.CraftBlock
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.TypeInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.isClass
import xyz.xenondevs.bytebase.util.replaceFirst
import xyz.xenondevs.nova.item.behavior.Tool
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.VanillaToolCategory
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.bukkitMirror
import xyz.xenondevs.nova.util.item.ToolUtils
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import net.minecraft.world.entity.player.Player as MojangPlayer
import net.minecraft.world.item.ItemStack as MojangStack

@Suppress("unused")
internal object ToolPatches : MultiTransformer(CraftBlock::class, MojangPlayer::class) {
    
    override fun transform() {
        transformCraftBlockIsPreferredTool()
        transformPlayerAttack()
        transformEnchantmentHelperGetKnockbackBonus()
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
    fun isPreferredTool(block: CraftBlock, blockState: BlockState, tool: MojangStack): Boolean {
        return !blockState.requiresCorrectToolForDrops() || ToolUtils.isCorrectToolForDrops(block, tool.bukkitMirror.takeUnlessEmpty())
    }
    
    /**
     * Patches the Player#attack method to use properly perform sweep attacks.
     */
    private fun transformPlayerAttack() {
        VirtualClassPath[MojangPlayer::attack]
            .replaceFirst(1, 0, buildInsnList {
                invokeStatic(::canDoSweepAttack)
            }) { it.opcode == Opcodes.INSTANCEOF && (it as TypeInsnNode).isClass(SwordItem::class) }
    }
    
    @JvmStatic
    fun canDoSweepAttack(itemStack: MojangStack): Boolean {
        val novaItem = itemStack.novaItem
        
        return if (novaItem != null) {
            novaItem.getBehaviorOrNull(Tool::class)?.canSweepAttack ?: false
        } else {
            (ToolCategory.ofItem(itemStack.bukkitMirror) as? VanillaToolCategory)?.canSweepAttack ?: false
        }
    }
    
    /**
     * Patches the EnchantmentHelper#getKnockbackBonus method to add the knockback bonus from Nova's tools.
     */
    private fun transformEnchantmentHelperGetKnockbackBonus() {
        VirtualClassPath[EnchantmentHelper::getKnockbackBonus].replaceInstructions {
            aLoad(0)
            invokeStatic(::getKnockbackBonus)
            ireturn()
        }
    }
    
    @JvmStatic
    fun getKnockbackBonus(entity: LivingEntity): Int {
        return EnchantmentHelper.getEnchantmentLevel(Enchantments.KNOCKBACK, entity) +
            (entity.mainHandItem.novaItem?.getBehaviorOrNull(Tool::class)?.knockbackBonus ?: 0)
    }
    
}