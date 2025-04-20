package xyz.xenondevs.nova.world.item.behavior

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Enchantable.enchantable
import io.papermc.paper.datacomponent.item.ItemEnchantments.itemEnchantments
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.map
import xyz.xenondevs.commons.provider.orElse
import xyz.xenondevs.nova.config.entryOrElse
import xyz.xenondevs.nova.config.optionalEntry
import xyz.xenondevs.nova.world.item.DataComponentMap
import xyz.xenondevs.nova.world.item.buildDataComponentMapProvider

/**
 * Creates a factory for [Enchantable] behaviors using the given values, if not specified otherwise in the item's config.
 *
 * @param enchantmentValue The enchantment value of the item.
 * A higher enchantment value brings more secondary and higher-level enchantments in the enchanting table.
 * Vanilla enchantment values: wood: 15, stone: 5, iron: 14, diamond: 10, gold: 22, netherite: 15
 * Used when `enchantment_value` is not specified in the item's config, or null to require the presence of a config entry.
 *
 * @param primaryEnchantments The enchantments that appear in the enchanting table.
 * Used when `primary_enchantments` is not specified in the item's config. Falls back to [supportedEnchantments] if unspecified.
 *
 * @param supportedEnchantments The enchantments that can be applied to the item, i.e. via an anvil or commands.
 * Used when `supported_enchantments` is not specified in the item's config, or null to require the presence of a config entry.
 */
@Suppress("FunctionName")
fun Enchantable(
    enchantmentValue: Int? = null,
    primaryEnchantments: Provider<Set<Enchantment>>? = null,
    supportedEnchantments: Provider<Set<Enchantment>>? = null
) = ItemBehaviorFactory<Enchantable> {
    val cfg = it.config
    
    val supportedEnchantments = cfg.entryOrElse(supportedEnchantments, "supported_enchantments")
    val primaryEnchantments = cfg.optionalEntry<Set<Enchantment>>("primary_enchantments").orElse(primaryEnchantments).orElse(supportedEnchantments)
    
    Enchantable(
        cfg.entryOrElse(enchantmentValue, "enchantment_value"),
        primaryEnchantments,
        supportedEnchantments
    )
}

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
    
    override val baseDataComponents: Provider<DataComponentMap> = buildDataComponentMapProvider { 
        this[DataComponentTypes.ENCHANTMENTS] = itemEnchantments().build()
        this[DataComponentTypes.ENCHANTABLE] = enchantmentValue.map(::enchantable)
    }
    
    override fun toString(itemStack: ItemStack): String {
        return "Enchantable(" +
            "enchantmentValue=$enchantmentValue, " +
            "primaryEnchantments=${primaryEnchantments.joinToString { it.key.toString() }}, " +
            "supportedEnchantments=${primaryEnchantments.joinToString { it.key.toString() }}" +
            ")"
    }
    
}