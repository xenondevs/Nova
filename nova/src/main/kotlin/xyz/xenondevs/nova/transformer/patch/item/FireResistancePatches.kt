@file:Suppress("unused")

package xyz.xenondevs.nova.transformer.patch.item

import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.replaceEvery
import xyz.xenondevs.nova.item.behavior.FireResistant
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.INVENTORY_HURT_ARMOR_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.ITEM_ENTITY_FIRE_IMMUNE_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.ITEM_IS_FIRE_RESISTANT_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionUtils

internal object FireResistancePatches : MultiTransformer(setOf(ItemEntity::class, Inventory::class), computeFrames = true) {
    
    override fun transform() {
        ITEM_ENTITY_FIRE_IMMUNE_METHOD.replaceWith(
            ReflectionUtils.getMethodByName(FireResistancePatches::class, false, "isFireImmune")
        )
        
        VirtualClassPath[INVENTORY_HURT_ARMOR_METHOD].replaceEvery(
            1, 0,
            buildInsnList {
                invokeStatic(ReflectionUtils.getMethodByName(FireResistancePatches::class, false, "isFireResistant"))
            }
        ) { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(ITEM_IS_FIRE_RESISTANT_METHOD) }
    }
    
    @JvmStatic
    fun ItemEntity.isFireImmune(): Boolean {
        return isFireResistant(item)
    }
    
    @JvmStatic
    fun isFireResistant(itemStack: ItemStack): Boolean {
        val novaMaterial = itemStack.novaMaterial
        if (novaMaterial != null)
            return novaMaterial.itemLogic.hasBehavior(FireResistant::class)
        
        return itemStack.item.isFireResistant
    }
    
}