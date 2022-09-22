package xyz.xenondevs.nova.transformer.patch.item

import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import net.minecraft.core.Registry
import net.minecraft.stats.Stats
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.craftbukkit.v1_19_R1.block.CraftBlock
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.TypeInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.bytebase.util.replaceFirst
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.bukkitMirror
import xyz.xenondevs.nova.util.item.ToolDamageResult
import xyz.xenondevs.nova.util.item.ToolUtils
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.item.takeUnlessAir
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.resourceLocation
import java.util.function.Consumer
import kotlin.reflect.jvm.javaMethod
import net.minecraft.world.entity.player.Player as MojangPlayer
import net.minecraft.world.item.ItemStack as MojangStack

internal object ToolPatches : MultiTransformer(setOf(CraftBlock::class, MojangStack::class, MojangPlayer::class), true) {
    
    override fun transform() {
        transformCraftBlockIsPreferredTool()
        transformItemStackGetAttributeModifiers()
        transformItemStackHurtAndBreak()
        transformItemStackHurtEnemy()
        transformPlayerAttack()
    }
    
    /**
     * Patches the CraftBlock#isPreferredTool method to properly handle Nova's tools.
     */
    private fun transformCraftBlockIsPreferredTool() {
        classWrappers[CraftBlock::class.internalName]!!
            .getMethodLike(ReflectionRegistry.CRAFT_BLOCK_IS_PREFERRED_TOOL_METHOD)!!
            .instructions = buildInsnList {
            aLoad(0)
            aLoad(1)
            aLoad(2)
            invokeStatic(::isPreferredTool.javaMethod!!)
            ireturn()
        }
    }
    
    @JvmStatic
    fun isPreferredTool(block: CraftBlock, blockState: BlockState, tool: MojangStack): Boolean {
        return !blockState.requiresCorrectToolForDrops() || ToolUtils.isCorrectToolForDrops(tool.bukkitMirror.takeUnlessAir(), block)
    }
    
    /**
     * Patches the ItemStack#getAttributeModifiers to return to correct modifiers for Nova's tools.
     */
    private fun transformItemStackGetAttributeModifiers() {
        classWrappers[MojangStack::class.internalName]!!
            .getMethodLike(ReflectionRegistry.ITEM_STACK_GET_ATTRIBUTE_MODIFIERS_METHOD)!!
            .replaceFirst(2, 0, buildInsnList {
                aLoad(0)
                aLoad(2)
                checkCast(Multimap::class.internalName)
                invokeStatic(::modifyAttributeModifiers.javaMethod!!)
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
     * Patches the ItemStack#hurtAndBreak method to properly damage Nova's tools.
     */
    private fun transformItemStackHurtAndBreak() {
        classWrappers[MojangStack::class.internalName]!!
            .getMethodLike(ReflectionRegistry.ITEM_STACK_HURT_AND_BREAK_METHOD)!!
            .instructions = buildInsnList {
            aLoad(0)
            iLoad(1)
            aLoad(2)
            aLoad(3)
            invokeStatic(::hurtAndBreak.javaMethod!!)
            _return()
        }
    }
    
    @JvmStatic
    fun hurtAndBreak(itemStack: MojangStack, damage: Int, entity: LivingEntity, consumer: Consumer<LivingEntity>) {
        if (ToolUtils.damageAndBreakTool(itemStack, damage, entity) == ToolDamageResult.BROKEN) {
            consumer.accept(entity)
        }
    }
    
    /**
     * Patches the ItemStack#hurtEnemy method to properly damage Nova's tools and with the proper damage values
     * defined in their tool category.
     */
    private fun transformItemStackHurtEnemy() {
        classWrappers[MojangStack::class.internalName]!!
            .getMethodLike(ReflectionRegistry.ITEM_STACK_HURT_ENTITY_METHOD)!!
            .instructions = buildInsnList {
            aLoad(0)
            aLoad(2)
            invokeStatic(::hurtEnemy.javaMethod!!)
            _return()
        }
    }
    
    @JvmStatic
    fun hurtEnemy(itemStack: MojangStack, player: MojangPlayer) {
        val damage = ToolCategory.ofItem(itemStack.bukkitMirror)?.attackEntityItemDamage ?: return
        
        if (itemStack.novaMaterial == null)
            player.awardStat(Stats.ITEM_USED.get(itemStack.item))
        
        itemStack.hurtAndBreak(damage, player) {
            player.broadcastBreakEvent(EquipmentSlot.MAINHAND)
        }
    }
    
    /**
     * Patches the Player#attack method to use [ToolCategory.canDoSweepAttack] instead of the default sword check
     */
    private fun transformPlayerAttack() {
        classWrappers[MojangPlayer::class.internalName]!!
            .getMethodLike(ReflectionRegistry.PLAYER_ATTACK_METHOD)!!
            .replaceFirst(1, 0, buildInsnList {
                invokeStatic(::canDoSweepAttack.javaMethod!!)
            }) { it.opcode == Opcodes.INSTANCEOF && (it as TypeInsnNode).desc == "SRC/(net.minecraft.world.item.SwordItem)" }
    }
    
    @JvmStatic
    fun canDoSweepAttack(itemStack: MojangStack): Boolean {
        return ToolCategory.ofItem(itemStack.bukkitMirror)?.canDoSweepAttack ?: false
    }
    
}