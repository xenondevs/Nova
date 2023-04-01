@file:Suppress("unused")

package xyz.xenondevs.nova.transformer.patch.item

import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.replaceEvery
import xyz.xenondevs.nova.item.behavior.FireResistant
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.item.novaItem

internal object FireResistancePatches : MultiTransformer(ItemEntity::class, Inventory::class) {
    
    override fun transform() {
        ItemEntity::fireImmune.replaceWith(::isFireImmune)
        
        VirtualClassPath[Inventory::hurtArmor].replaceEvery(
            1, 0,
            buildInsnList { invokeStatic(::isFireResistant) }
        ) { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(Item::isFireResistant) }
    }
    
    @JvmStatic
    fun isFireImmune(item: ItemEntity): Boolean {
        return isFireResistant(item.item)
    }
    
    @JvmStatic
    fun isFireResistant(itemStack: ItemStack): Boolean {
        val novaMaterial = itemStack.novaItem
        if (novaMaterial != null)
            return novaMaterial.itemLogic.hasBehavior(FireResistant::class)
        
        return itemStack.item.isFireResistant
    }
    
}