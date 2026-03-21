package xyz.xenondevs.nova.registry

import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.text.Component
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.world.item.behavior.Enchantable

/**
 * A builder for enchantments.
 */
@RegistryElementBuilderDsl
sealed interface EnchantmentBuilder {
    
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
     * Sets the items for which this enchantment can show up in the enchanting table.
     *
     * For your own custom items, prefer configuring the supported enchantments via
     * the [Enchantable] item behavior.
     */
    fun enchantsPrimary(canEnchant: (ItemStack) -> Boolean)
    
    /**
     * Sets the items to which this enchantment can be applied to, for example in an anvil.
     *
     * To have the enchantment appear in the enchanting table, use [enchantsPrimary].
     *
     * For your own custom items, prefer configuring the supported enchantments via
     * the [Enchantable] item behavior.
     */
    fun enchants(canEnchant: (ItemStack) -> Boolean)
    
    /**
     * Sets the compatibility of this enchantment with other enchantments.
     *
     * This option is exclusive with [compatibleWith] and [incompatibleWith].
     */
    fun compatibility(compatibility: (Enchantment) -> Boolean)
    
    /**
     * Defines with which enchantments this enchantment is compatible. All other enchantments are incompatible.
     *
     * This option is exclusive with [compatibility] and [incompatibleWith].
     */
    fun compatibleWith(vararg enchantments: TypedKey<Enchantment>)
    
    /**
     * Sets the compatibility of this enchantment with other enchantments.
     *
     * This option is exclusive with [compatibility] and [compatibleWith].
     */
    fun incompatibleWith(vararg enchantments: TypedKey<Enchantment>)
    
}