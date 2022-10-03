package xyz.xenondevs.nova.transformer.patch.item

import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import net.minecraft.core.Registry
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.craftbukkit.v1_19_R1.block.CraftBlock
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.TypeInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.bytebase.util.replaceFirst
import xyz.xenondevs.nova.item.behavior.Tool
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.VanillaToolCategory
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.bukkitMirror
import xyz.xenondevs.nova.util.item.ToolUtils
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.item.takeUnlessAir
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getMethodByName
import xyz.xenondevs.nova.util.resourceLocation
import net.minecraft.world.entity.player.Player as MojangPlayer
import net.minecraft.world.item.ItemStack as MojangStack

@Suppress("unused")
internal object ToolPatches : MultiTransformer(setOf(CraftBlock::class, MojangStack::class, MojangPlayer::class), true) {
    
    override fun transform() {
        transformCraftBlockIsPreferredTool()
        transformItemStackGetAttributeModifiers()
        transformPlayerAttack()
    }
    
    /**
     * Patches the CraftBlock#isPreferredTool method to properly handle Nova's tools.
     */
    private fun transformCraftBlockIsPreferredTool() {
        VirtualClassPath[ReflectionRegistry.CRAFT_BLOCK_IS_PREFERRED_TOOL_METHOD]
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
        return !blockState.requiresCorrectToolForDrops() || ToolUtils.isCorrectToolForDrops(block, tool.bukkitMirror.takeUnlessAir())
    }
    
    /**
     * Patches the ItemStack#getAttributeModifiers to return to correct modifiers for Nova's tools.
     */
    private fun transformItemStackGetAttributeModifiers() {
        VirtualClassPath[ReflectionRegistry.ITEM_STACK_GET_ATTRIBUTE_MODIFIERS_METHOD]
            .replaceFirst(2, 0, buildInsnList {
                aLoad(0)
                aLoad(2)
                checkCast(Multimap::class.internalName)
                invokeStatic(getMethodByName(ToolPatches::class.java, false, "modifyAttributeModifiers"))
                areturn()
            }) { it.opcode == Opcodes.ARETURN }
    }
    
    @JvmStatic
    fun modifyAttributeModifiers(itemStack: MojangStack, modifiers: Multimap<Attribute, AttributeModifier>): Multimap<Attribute, AttributeModifier> {
        val attributeModifiers = itemStack.novaMaterial?.novaItem
            ?.getAttributeModifiers()?.takeUnless(List<*>::isEmpty)
            ?: return modifiers
        
        val novaModifiers = Multimaps.newListMultimap<Attribute, AttributeModifier>(HashMap(), ::ArrayList)
        
        // copy previous modifiers
        novaModifiers.putAll(modifiers)
        
        // add new nova modifiers
        attributeModifiers.forEach {
            novaModifiers.put(
                Registry.ATTRIBUTE.get(it.attribute.key.resourceLocation),
                AttributeModifier(it.uuid, it.uuid.toString(), it.value, AttributeModifier.Operation.values()[it.operation.ordinal])
            )
        }
        
        return novaModifiers
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
    
}