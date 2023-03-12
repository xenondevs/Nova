package xyz.xenondevs.nova.transformer.patch.item

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.craftbukkit.v1_19_R2.block.CraftBlock
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.TypeInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.replaceFirst
import xyz.xenondevs.nova.item.behavior.Tool
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.VanillaToolCategory
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.bukkitMirror
import xyz.xenondevs.nova.util.item.ToolUtils
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getMethodByName
import net.minecraft.world.entity.player.Player as MojangPlayer
import net.minecraft.world.item.ItemStack as MojangStack

@Suppress("unused")
internal object ToolPatches : MultiTransformer(setOf(CraftBlock::class, MojangPlayer::class), true) {
    
    override fun transform() {
        transformCraftBlockIsPreferredTool()
        transformPlayerAttack()
        transformEnchantmentHelperGetKnockbackBonus()
    }
    
    /**
     * Patches the CraftBlock#isPreferredTool method to properly handle Nova's tools.
     */
    private fun transformCraftBlockIsPreferredTool() {
        VirtualClassPath[ReflectionRegistry.CRAFT_BLOCK_DATA_IS_PREFERRED_TOOL_METHOD]
            .instructions = buildInsnList {
            aLoad(0)
            aLoad(1)
            aLoad(2)
            invokeStatic(getMethodByName(ToolPatches::class.java, false, "isPreferredTool"))
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
        VirtualClassPath[ReflectionRegistry.PLAYER_ATTACK_METHOD]
            .replaceFirst(1, 0, buildInsnList {
                invokeStatic(getMethodByName(ToolPatches::class.java, false, "canDoSweepAttack"))
            }) { it.opcode == Opcodes.INSTANCEOF && (it as TypeInsnNode).desc == "SRC/(net.minecraft.world.item.SwordItem)" }
    }
    
    @JvmStatic
    fun canDoSweepAttack(itemStack: MojangStack): Boolean {
        val novaMaterial = itemStack.novaMaterial
        
        return if (novaMaterial != null) {
            novaMaterial.novaItem.getBehavior(Tool::class)?.options?.canSweepAttack ?: false
        } else {
            (ToolCategory.ofItem(itemStack.bukkitMirror) as? VanillaToolCategory)?.canSweepAttack ?: false
        }
    }
    
    /**
     * Patches the EnchantmentHelper#getKnockbackBonus method to add the knockback bonus from Nova's tools.
     */
    private fun transformEnchantmentHelperGetKnockbackBonus() {
        VirtualClassPath[ReflectionRegistry.ENCHANTMENT_HELPER_GET_KNOCKBACK_BONUS_METHOD].instructions = buildInsnList {
            aLoad(0)
            invokeStatic(getMethodByName(ToolPatches::class.java, false, "getKnockbackBonus"))
            ireturn()
        }
    }
    
    @JvmStatic
    fun getKnockbackBonus(entity: LivingEntity): Int {
        return EnchantmentHelper.getEnchantmentLevel(Enchantments.KNOCKBACK, entity) +
            (entity.mainHandItem.novaMaterial?.novaItem?.getBehavior(Tool::class)?.options?.knockbackBonus ?: 0)
    }
    
}