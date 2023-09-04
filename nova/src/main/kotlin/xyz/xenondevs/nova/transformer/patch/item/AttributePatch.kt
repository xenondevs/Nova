package xyz.xenondevs.nova.transformer.patch.item

import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.replaceFirst
import xyz.xenondevs.nova.transformer.MethodTransformer
import xyz.xenondevs.nova.util.item.novaItem

internal object AttributePatch : MethodTransformer(ItemStack::getAttributeModifiers) {
    
    /**
     * Patches the ItemStack#getAttributeModifiers to return to correct modifiers for Nova's items.
     */
    override fun transform() {
        methodNode.replaceFirst(2, 0, buildInsnList {
            aLoad(1)
            invokeStatic(::getDefaultAttributeModifiers)
        }) { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(Item::getDefaultAttributeModifiers) }
    }
    
    @JvmStatic
    fun getDefaultAttributeModifiers(itemStack: ItemStack, slot: EquipmentSlot): Multimap<Attribute, AttributeModifier> {
        val novaItem = itemStack.novaItem
        if (novaItem != null) {
            val attributeModifiers = novaItem.attributeModifiers[slot]?.takeUnless(List<*>::isEmpty)
            val novaModifiers = Multimaps.newListMultimap<Attribute, AttributeModifier>(HashMap(), ::ArrayList)
            attributeModifiers?.forEach {
                novaModifiers.put(
                    it.attribute,
                    AttributeModifier(it.uuid, it.name, it.value, Operation.fromValue(it.operation.ordinal))
                )
            }
            
            return novaModifiers
        }
        
        return itemStack.item.getDefaultAttributeModifiers(slot)
    }
    
}