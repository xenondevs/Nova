package xyz.xenondevs.nova.transformer.patch.item

import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import net.minecraft.core.Registry
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.item.ItemStack
import org.objectweb.asm.Opcodes
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.bytebase.util.replaceFirst
import xyz.xenondevs.nova.transformer.MethodTransformer
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.util.resourceLocation

internal object AttributePatch : MethodTransformer(ReflectionRegistry.ITEM_STACK_GET_ATTRIBUTE_MODIFIERS_METHOD, computeFrames = true) {
    
    /**
     * Patches the ItemStack#getAttributeModifiers to return to correct modifiers for Nova's tools.
     */
    override fun transform() {
        methodNode
            .replaceFirst(2, 0, buildInsnList {
                aLoad(0)
                aLoad(1)
                aLoad(2)
                checkCast(Multimap::class.internalName)
                invokeStatic(ReflectionUtils.getMethodByName(AttributePatch::class.java, false, "modifyAttributeModifiers"))
                areturn()
            }) { it.opcode == Opcodes.ARETURN }
    }
    
    @JvmStatic
    fun modifyAttributeModifiers(itemStack: ItemStack, slot: EquipmentSlot, modifiers: Multimap<Attribute, AttributeModifier>): Multimap<Attribute, AttributeModifier> {
        val attributeModifiers = itemStack.novaMaterial?.novaItem
            ?.attributeModifiers
            ?.get(slot)
            ?.takeUnless(List<*>::isEmpty)
            ?: return modifiers
        
        val novaModifiers = Multimaps.newListMultimap<Attribute, AttributeModifier>(HashMap(), ::ArrayList)
        
        // copy previous modifiers
        novaModifiers.putAll(modifiers)
        
        // add new nova modifiers
        attributeModifiers.forEach {
            novaModifiers.put(
                Registry.ATTRIBUTE.get(it.attribute.key.resourceLocation),
                AttributeModifier(it.uuid, it.name, it.value, AttributeModifier.Operation.values()[it.operation.ordinal])
            )
        }
        
        return novaModifiers
    }
    
}