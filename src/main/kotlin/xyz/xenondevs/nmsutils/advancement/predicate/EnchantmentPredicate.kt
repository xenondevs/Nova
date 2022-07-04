package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.core.Registry
import org.bukkit.enchantments.Enchantment
import xyz.xenondevs.nmsutils.adapter.NonNullAdapter
import xyz.xenondevs.nmsutils.adapter.impl.IntBoundsAdapter
import xyz.xenondevs.nmsutils.advancement.AdvancementDsl
import xyz.xenondevs.nmsutils.util.resourceLocation
import net.minecraft.advancements.critereon.EnchantmentPredicate as MojangEnchantmentPredicate

class EnchantmentPredicate(
    val enchantment: Enchantment,
    val level: IntRange?
) : Predicate {
    
    companion object : NonNullAdapter<EnchantmentPredicate, MojangEnchantmentPredicate>(MojangEnchantmentPredicate.ANY) {
        
        override fun convert(value: EnchantmentPredicate): MojangEnchantmentPredicate {
            return MojangEnchantmentPredicate(
                Registry.ENCHANTMENT.get(value.enchantment.key.resourceLocation),
                IntBoundsAdapter.toNMS(value.level)
            )
        }
        
    }
    
    @AdvancementDsl
    class Builder {
        
        private var enchantment: Enchantment? = null
        private var level: IntRange? = null
        
        fun enchantment(value: Enchantment) {
            enchantment = value
        }
        
        fun level(value: IntRange) {
            level = value
        }
        
        internal fun build(): EnchantmentPredicate {
            checkNotNull(enchantment) { "Enchantment is not set" }
            return EnchantmentPredicate(enchantment!!, level)
        }
        
    }
    
}