package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.advancements.critereon.EnchantmentPredicate
import net.minecraft.advancements.critereon.MinMaxBounds
import net.minecraft.world.item.enchantment.Enchantment

class EnchantmentPredicateBuilder : PredicateBuilder<EnchantmentPredicate>() {
    
    private var enchantment: Enchantment? = null
    private var level = MinMaxBounds.Ints.ANY
    
    fun enchantment(value: Enchantment) {
        enchantment = value
    }
    
    fun level(value: MinMaxBounds.Ints) {
        level = value
    }
    
    fun level(value: IntRange) {
        level = MinMaxBounds.Ints.between(value.first, value.last)
    }
    
    fun level(value: Int) {
        level = MinMaxBounds.Ints.exactly(value)
    }
    
    override fun build(): EnchantmentPredicate {
        return EnchantmentPredicate(enchantment, level)
    }
    
}