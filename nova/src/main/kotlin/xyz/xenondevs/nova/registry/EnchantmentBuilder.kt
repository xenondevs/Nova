package xyz.xenondevs.nova.registry

import net.kyori.adventure.text.Component
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemType
import xyz.xenondevs.nova.world.item.behavior.Enchantable

/**
 * A builder for enchantments.
 */
@RegistryElementBuilderDsl
sealed interface EnchantmentBuilder : RegistryEntryBuilder.Paper<Enchantment> {
    
    /**
     * Sets the name of the enchantment.
     *
     * This function is exclusive with [localizedName].
     */
    fun name(name: Component)
    
    /**
     * Sets the localization key of the enchantment.
     *
     * Defaults to `enchantment.<namespace>.<name>`.
     */
    fun localizedName(localizedName: String) {
        name(Component.translatable(localizedName))
    }
    
    /**
     * Configures the maximum level of this enchantment. Defaults to `1`.
     */
    fun maxLevel(maxLevel: Int)
    
    /**
     * Configures the cost of this enchantment in an anvil. Defaults to `4`.
     */
    fun anvilCost(anvilCost: Int)
    
    /**
     * Configures the level range where the enchantment can appear in an enchanting table slot.
     */
    fun tableLevelRequirement(tableLeveRequirement: IntRange)
    
    /**
     * Configures the level range where the enchantment can appear in an enchanting table slot, based on the enchantment level.
     */
    fun tableLevelRequirement(tableLeveRequirement: (level: Int) -> IntRange)
    
    /**
     * The rarity of this enchantment. The value is used as a weight, so enchantments with a higher
     * value are more common. Defaults to `10`.
     *
     * Default vanilla rarities:
     *
     * - Common: 10
     * - Uncommon: 5
     * - Rare: 2
     * - Very rare: 1
     */
    fun rarity(weight: Int)
    
    /**
     * Whether this enchantment can appear in the enchanting table. Defaults to `false`.
     */
    fun tableDiscoverable(tableDiscoverable: Boolean)
    
    /**
     * Whether this enchantment is a treasure enchantment. Defaults to `false`.
     */
    fun treasure(treasure: Boolean)
    
    /**
     * Whether this enchantment can be traded with villagers. Defaults to `false`.
     */
    fun tradeable(tradeable: Boolean)
    
    /**
     * Whether this enchantment is a curse enchantment. Defaults to `false`.
     */
    fun curse(curse: Boolean)
    
    /**
     * Sets the item types for which this enchantment can show up in the enchanting table (if [tableDiscoverable]).
     * If unspecified, defaults to the set of item types defined in [enchants].
     *
     * For your own custom items, you can also configure the supported enchantments via the [Enchantable] item behavior.
     */
    fun enchantsPrimary(items: RegistryEntrySet.Paper<ItemType>)
    
    /**
     * Sets the item types to which this enchantment can be applied to, for example, in an anvil.
     *
     * To have the enchanment appear in the enchanting table for some item types and only be applicable
     * via the anvil for others, define [enchants] for all supported item types, then [enchantsPrimary]
     * for all item types for which the enchantment is supposed to show up in the enchanting table
     * and set [tableDiscoverable] to `true`. If you need no such distinction, only define the [enchants]
     * item type set and they will be considered "primary" as well.
     * 
     * For your own custom items, you can also configure the supported enchantments via
     * the [Enchantable] item behavior.
     */
    fun enchants(items: RegistryEntrySet.Paper<ItemType>)
    
    /**
     * Sets the enchantments that are incompatible (exclusive) with this enchantment.
     */
    fun incompatibleWith(enchantments: RegistryEntrySet.Paper<Enchantment>)
    
}