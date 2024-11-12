package xyz.xenondevs.nova.world.item.behavior

import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.enchantment.ItemEnchantments
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.map
import xyz.xenondevs.commons.provider.orElse
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.config.optionalEntry
import xyz.xenondevs.nova.world.item.NovaItem
import net.minecraft.world.item.enchantment.Enchantable as EnchantableComponent

/**
 * Makes an item enchantable.
 *
 * @param enchantmentValue The enchantment value of the item.
 * A higher enchantment value brings more secondary and higher-level enchantments in the enchanting table.
 * Vanilla enchantment values: wood: 15, stone: 5, iron: 14, diamond: 10, gold: 22, netherite: 15
 * @param primaryEnchantments The enchantments that appear in the enchanting table.
 * @param supportedEnchantments The enchantments that can be applied to the item, i.e. via an anvil or commands.
 */
class Enchantable(
    enchantmentValue: Provider<Int>,
    primaryEnchantments: Provider<Set<Enchantment>>,
    supportedEnchantments: Provider<Set<Enchantment>>
) : ItemBehavior {
    
    /**
     * The enchantment value of the item.
     * A higher enchantment value brings more secondary and higher-level enchantments in the enchanting table.
     * Vanilla enchantment values: wood: 15, stone: 5, iron: 14, diamond: 10, gold: 22, netherite: 15
     */
    val enchantmentValue by enchantmentValue
    
    /**
     * The enchantments that appear in the enchanting table.
     */
    val primaryEnchantments by primaryEnchantments
    
    /**
     * The enchantments that can be applied to the item, i.e. via an anvil or commands.
     */
    val supportedEnchantments by supportedEnchantments
    
    override val baseDataComponents = enchantmentValue.map { enchantmentValue ->
        DataComponentMap.builder()
            .set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
            .set(DataComponents.ENCHANTABLE, EnchantableComponent(enchantmentValue))
            .build()
    }
    
    override fun toString(itemStack: ItemStack): String {
        return "Enchantable(" +
            "enchantmentValue=$enchantmentValue, " +
            "primaryEnchantments=${primaryEnchantments.joinToString { it.key.toString() }}, " +
            "supportedEnchantments=${primaryEnchantments.joinToString { it.key.toString() }}" +
            ")"
    }
    
    companion object : ItemBehaviorFactory<Enchantable> {
        
        override fun create(item: NovaItem): Enchantable {
            val cfg = item.config
            val supportedEnchantments = cfg.optionalEntry<Set<Enchantment>>("supported_enchantments").orElse(emptySet())
            return Enchantable(
                cfg.entry("enchantment_value"),
                cfg.optionalEntry<Set<Enchantment>>("primary_enchantments").orElse(supportedEnchantments),
                supportedEnchantments
            )
        }
        
    }
    
}