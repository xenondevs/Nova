package xyz.xenondevs.nova.world.item.enchantment

import net.minecraft.core.Holder
import net.minecraft.world.item.enchantment.Enchantment
import org.bukkit.inventory.ItemStack
import java.util.*
import net.minecraft.world.item.enchantment.Enchantment as MojangEnchantment

internal class CustomEnchantmentLogic(
    private val primaryItem: (ItemStack) -> Boolean,
    private val supportedItem: (ItemStack) -> Boolean,
    private val tableLevelRequirement: (Int) -> IntRange,
    private val compatibility: (Holder<MojangEnchantment>) -> Boolean
) {
    
    fun isPrimaryItem(itemStack: ItemStack): Boolean =
        primaryItem(itemStack)
    
    fun isSupportedItem(itemStack: ItemStack): Boolean =
        supportedItem(itemStack)
    
    fun compatibleWith(other: Holder<MojangEnchantment>): Boolean =
        compatibility(other)
    
    fun getMinCost(level: Int): Int =
        tableLevelRequirement(level).first
    
    fun getMaxCost(level: Int): Int =
        tableLevelRequirement(level).last
    
    companion object {
        
        @JvmField
        val customEnchantments = IdentityHashMap<Enchantment, CustomEnchantmentLogic>()
        
    }
    
}