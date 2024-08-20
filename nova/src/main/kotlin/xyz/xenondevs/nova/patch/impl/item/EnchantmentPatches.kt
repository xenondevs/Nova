package xyz.xenondevs.nova.patch.impl.item

import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentHelper
import org.bukkit.craftbukkit.enchantments.CraftEnchantment
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.replaceFirstRange
import xyz.xenondevs.nova.patch.MultiTransformer
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.world.item.behavior.Enchantable
import xyz.xenondevs.nova.world.item.enchantment.CustomEnchantmentLogic
import java.util.*

internal object EnchantmentPatches : MultiTransformer(Enchantment::class, EnchantmentHelper::class, ItemStack::class) {
    
    val customEnchantments = IdentityHashMap<Enchantment, CustomEnchantmentLogic>()
    
    override fun transform() {
        VirtualClassPath[Enchantment::isPrimaryItem].delegateStatic(::isPrimaryItem)
        VirtualClassPath[Enchantment::isSupportedItem].delegateStatic(::isSupportedItem)
        VirtualClassPath[Enchantment::canEnchant].delegateStatic(::isSupportedItem)
        VirtualClassPath[Enchantment::getMinCost].delegateStatic(::getMinCost)
        VirtualClassPath[Enchantment::getMaxCost].delegateStatic(::getMaxCost)
        VirtualClassPath[Enchantment::areCompatible].delegateStatic(::areCompatible)
        VirtualClassPath[ItemStack::isEnchantable].delegateStatic(::isEnchantable)
        patchEnchantmentCostRetrieval(VirtualClassPath[EnchantmentHelper::getEnchantmentCost])
        patchEnchantmentCostRetrieval(VirtualClassPath[EnchantmentHelper::selectEnchantment])
    }
    
    private fun patchEnchantmentCostRetrieval(methodNode: MethodNode) {
        // EnchantmentHelper::getEnchantmentCost and EnchantmentHelper::selectEnchantment only use Item instance to retrieve enchantmentValue
        
        methodNode.replaceFirstRange(
            { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(ItemStack::getItem) },
            { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(Item::getEnchantmentValue) },
            0, 0,
            buildInsnList {
                invokeStatic(::getEnchantmentValue)
            }
        )
    }
    
    @JvmStatic
    fun getEnchantmentValue(itemStack: ItemStack): Int {
        val novaItem = itemStack.novaItem
        if (novaItem != null) {
            return novaItem.getBehaviorOrNull<Enchantable>()?.enchantmentValue ?: 0
        }
        
        return itemStack.item.enchantmentValue
    }
    
    @JvmStatic
    fun isPrimaryItem(enchantment: Enchantment, itemStack: ItemStack): Boolean {
        if (customEnchantments[enchantment]?.isPrimaryItem(itemStack.asBukkitMirror()) == true)
            return true
        
        val novaItem = itemStack.novaItem
        if (novaItem != null) {
            val bukkitEnchantment = CraftEnchantment.minecraftToBukkit(enchantment)
            return novaItem.getBehaviorOrNull<Enchantable>()
                ?.primaryEnchantments
                ?.contains(bukkitEnchantment) == true
        } else {
            return isSupportedItem(enchantment, itemStack)
                && (enchantment.definition.primaryItems.isEmpty || itemStack.`is`(enchantment.definition.primaryItems.get()))
        }
    }
    
    @JvmStatic
    fun isSupportedItem(enchantment: Enchantment, itemStack: ItemStack): Boolean {
        if (customEnchantments[enchantment]?.isSupportedItem(itemStack.asBukkitMirror()) == true)
            return true
        
        val novaItem = itemStack.novaItem
        if (novaItem != null) {
            val bukkitEnchantment = CraftEnchantment.minecraftToBukkit(enchantment)
            return novaItem.getBehaviorOrNull<Enchantable>()
                ?.supportedEnchantments
                ?.contains(bukkitEnchantment) == true
        } else {
            return itemStack.`is`(enchantment.definition.supportedItems)
        }
    }
    
    @JvmStatic
    fun getMinCost(enchantment: Enchantment, level: Int): Int {
        if (enchantment in customEnchantments)
            return customEnchantments[enchantment]!!.getMinCost(level)
        
        // nms logic
        return enchantment.definition.minCost.calculate(level)
    }
    
    @JvmStatic
    fun getMaxCost(enchantment: Enchantment, level: Int): Int {
        if (enchantment in customEnchantments)
            return customEnchantments[enchantment]!!.getMaxCost(level)
        
        // nms logic
        return enchantment.definition.maxCost.calculate(level)
    }
    
    @JvmStatic
    fun areCompatible(first: Holder<Enchantment>, second: Holder<Enchantment>): Boolean {
        if (first == second)
            return false
        
        val firstCompatSecond: Boolean
        if (first.value() in customEnchantments) {
            firstCompatSecond = customEnchantments[first.value()]!!.compatibleWith(second.value())
        } else {
            firstCompatSecond = second !in first.value().exclusiveSet
        }
        
        val secondCompatFirst: Boolean
        if (second.value() in customEnchantments) {
            secondCompatFirst = customEnchantments[second.value()]!!.compatibleWith(first.value())
        } else {
            secondCompatFirst = first !in second.value().exclusiveSet
        }
        
        return firstCompatSecond && secondCompatFirst
    }
    
    @JvmStatic
    fun isEnchantable(itemStack: ItemStack): Boolean {
        val enchantments = itemStack.get(DataComponents.ENCHANTMENTS)
        if (enchantments == null || !enchantments.isEmpty)
            return false
        
        val novaItem = itemStack.novaItem
        if (novaItem != null) {
            return novaItem.hasBehavior<Enchantable>()
        } else {
            return itemStack.item.isEnchantable(itemStack)
        }
    }
    
}