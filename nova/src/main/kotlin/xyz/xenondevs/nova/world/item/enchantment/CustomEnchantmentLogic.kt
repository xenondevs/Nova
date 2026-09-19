package xyz.xenondevs.nova.world.item.enchantment

import net.minecraft.world.item.enchantment.Enchantment
import xyz.xenondevs.commons.provider.Provider
import java.util.*

internal class CustomEnchantmentLogic(
    private val tableLevelRequirement: Provider<(Int) -> IntRange>,
) {
    
    fun getMinCost(level: Int): Int =
        tableLevelRequirement.get()(level).first
    
    fun getMaxCost(level: Int): Int =
        tableLevelRequirement.get()(level).last
    
    companion object {
        
        @JvmField
        val customEnchantments = IdentityHashMap<Enchantment, CustomEnchantmentLogic>()
        
    }
    
}