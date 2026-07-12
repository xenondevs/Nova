package xyz.xenondevs.nova.world.item.enchantment

import net.minecraft.world.item.enchantment.Enchantment
import java.util.*

internal class CustomEnchantmentLogic(
    private val tableLevelRequirement: (Int) -> IntRange,
) {
    
    fun getMinCost(level: Int): Int =
        tableLevelRequirement(level).first
    
    fun getMaxCost(level: Int): Int =
        tableLevelRequirement(level).last
    
    companion object {
        
        @JvmField
        val customEnchantments = IdentityHashMap<Enchantment, CustomEnchantmentLogic>()
        
    }
    
}