package xyz.xenondevs.nova.transformer.patch.item

import net.minecraft.world.item.enchantment.EnchantmentCategory
import net.minecraft.world.item.enchantment.EnchantmentHelper
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.util.callsMethod
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.bytebase.util.replaceFirst
import xyz.xenondevs.nova.item.behavior.Enchantable
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import kotlin.reflect.jvm.javaMethod
import net.minecraft.world.item.Item as MojangItem
import net.minecraft.world.item.ItemStack as MojangStack

internal object EnchantmentPatches : MultiTransformer(setOf(EnchantmentHelper::class, MojangItem::class), computeFrames = true) {
    
    override fun transform() {
        val enchantmentHelperWrapper = classWrappers[EnchantmentHelper::class.internalName]!!
        
        enchantmentHelperWrapper
            .getMethodLike(ReflectionRegistry.ENCHANTMENT_HELPER_GET_AVAILABLE_ENCHANTMENT_RESULTS_METHOD)!!
            .replaceFirst(1, 0, buildInsnList {
                aLoad(1)
                invokeStatic(EnchantmentPatches::canEnchantItemWith.javaMethod!!)
            }) { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).callsMethod(ReflectionRegistry.ENCHANTMENT_CATEGORY_CAN_ENCHANT_METHOD) }
        
        val enchantmentValueAccessingMethods = mapOf(
            enchantmentHelperWrapper.getMethodLike(ReflectionRegistry.ENCHANTMENT_HELPER_GET_ENCHANTMENT_COST_METHOD)!! to 3,
            enchantmentHelperWrapper.getMethodLike(ReflectionRegistry.ENCHANTMENT_HELPER_SELECT_ENCHANTMENT_METHOD)!! to 1
        )
        
        enchantmentValueAccessingMethods.forEach { (method, localIdx) ->
            method.replaceFirst(1, 0, buildInsnList {
                aLoad(localIdx)
                invokeStatic(::getEnchantmentValue.javaMethod!!)
            }) { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).callsMethod(ReflectionRegistry.ITEM_GET_ENCHANTMENT_VALUE_METHOD) }
        }
        
        classWrappers[MojangItem::class.internalName]!!
            .getMethodLike(ReflectionRegistry.ITEM_IS_ENCHANTABLE_METHOD)!!
            .instructions = buildInsnList {
            aLoad(1)
            aLoad(0)
            invokeStatic(EnchantmentPatches::isEnchantable.javaMethod!!)
            ireturn()
        }
    }
    
    @JvmStatic
    fun canEnchantItemWith(category: EnchantmentCategory, itemStack: MojangStack): Boolean {
        val novaMaterial = itemStack.novaMaterial
        if (novaMaterial != null) {
            val categories = itemStack.novaMaterial?.novaItem?.getBehavior(Enchantable::class)?.options?.enchantmentCategories
            return categories != null && category in categories
        }
        
        return category.canEnchant(itemStack.item)
    }
    
    @JvmStatic
    fun getEnchantmentValue(itemStack: MojangStack): Int {
        val novaMaterial = itemStack.novaMaterial
        if (novaMaterial != null) {
            return novaMaterial.novaItem.getBehavior(Enchantable::class)?.options?.enchantmentValue ?: 0
        }
        
        return itemStack.item.enchantmentValue
    }
    
    @JvmStatic
    fun isEnchantable(itemStack: MojangStack, item: MojangItem): Boolean {
        val novaMaterial = itemStack.novaMaterial
        if (novaMaterial != null) {
            return novaMaterial.novaItem.hasBehavior(Enchantable::class)
        }
        
        return item.maxStackSize == 1 && item.canBeDepleted()
    }
    
}