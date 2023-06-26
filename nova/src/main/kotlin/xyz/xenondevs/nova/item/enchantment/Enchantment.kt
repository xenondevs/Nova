package xyz.xenondevs.nova.item.enchantment

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.enchantment.Enchantment.Rarity
import org.bukkit.enchantments.EnchantmentTarget
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.RegistryElementBuilder
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.getOrThrow
import xyz.xenondevs.nova.util.namespacedKey
import xyz.xenondevs.nova.util.resourceLocation
import net.minecraft.world.item.enchantment.Enchantment as MojangEnchantment
import org.bukkit.enchantments.Enchantment as BukkitEnchantment

sealed interface Enchantment {
    
    /**
     * The id of this enchantment.
     */
    val id: ResourceLocation
    
    /**
     * The localization key of this enchantment.
     */
    val localizedName: String
    
    /**
     * The minimum level of this enchantment.
     */
    val minLevel: Int
    
    /**
     * The maximum level of this enchantment.
     */
    val maxLevel: Int
    
    /**
     * The rarity of this enchantment as a weight. A higher number means that this enchantment is more common.
     */
    val rarity: Int
    
    /**
     * Whether this enchantment can appear in the enchantment table.
     */
    val isTableDiscoverable: Boolean
    
    /**
     * Whether this enchantment is a treasure.
     */
    val isTreasure: Boolean
    
    /**
     * Whether this enchantment can be obtained through trading.
     */
    val isTradeable: Boolean
    
    /**
     * Whether this enchantment is a curse.
     */
    val isCurse: Boolean
    
    /**
     * Returns a range of valid levels that qualify an enchantment table slot to show an enchantment of [level].
     * This is sometimes also referred to as "cost", but it is not the actual cost of the enchantment.
     */
    fun getTableLevelRequirement(level: Int): IntRange
    
    /**
     * Checks whether this enchantment is compatible with the given enchantment.
     */
    fun isCompatibleWith(other: Enchantment): Boolean
    
    companion object {
        
        /**
         * Retrieves the related [Enchantment] for the given [MojangEnchantment] from the [NovaRegistries.ENCHANTMENT] registry.
         */
        fun of(enchantment: MojangEnchantment): Enchantment {
            val id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment)!!
            return NovaRegistries.ENCHANTMENT.getOrThrow(id)
        }
        
        /**
         * Retrieves the related [Enchantment] for the given [BukkitEnchantment] from the [NovaRegistries.ENCHANTMENT] registry.
         */
        fun of(enchantment: BukkitEnchantment): Enchantment {
            val id = enchantment.key.resourceLocation
            return NovaRegistries.ENCHANTMENT.getOrThrow(id)
        }
        
        /**
         * Returns the corresponding [BukkitEnchantment] or creates a wrapper if [enchantment] is a custom enchantment.
         */
        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        fun asBukkitEnchantment(enchantment: Enchantment): BukkitEnchantment {
            val key = enchantment.id.namespacedKey
            if (enchantment is VanillaEnchantment)
                return BukkitEnchantment.getByKey(key)!!
            
            return object : BukkitEnchantment(key) {
                
                override fun getName() = key.toString()
                override fun getMaxLevel(): Int = enchantment.maxLevel
                override fun getStartLevel(): Int = enchantment.minLevel
                override fun getItemTarget(): EnchantmentTarget = EnchantmentTarget.ALL
                override fun isTreasure(): Boolean = false
                override fun isCursed(): Boolean = false
                
                override fun conflictsWith(other: BukkitEnchantment): Boolean =
                    !enchantment.isCompatibleWith(of(other))
                
                override fun canEnchantItem(itemStack: ItemStack): Boolean =
                    NovaRegistries.ENCHANTMENT_CATEGORY.asSequence()
                        .filter { enchantment in it.enchantments }
                        .any { it.canEnchant(itemStack) }
                
            }
        }
        
    }
    
}

internal class NovaEnchantment(
    override val id: ResourceLocation,
    override val minLevel: Int,
    override val maxLevel: Int,
    override val rarity: Int,
    override val isTableDiscoverable: Boolean,
    override val isTreasure: Boolean,
    override val isTradeable: Boolean,
    override val isCurse: Boolean,
    private val levelRequirement: (Int) -> IntRange,
    private val compatibility: (Enchantment) -> Boolean
) : Enchantment {
    
    override val localizedName = "enchantment.${id.namespace}.${id.path}"
    
    override fun getTableLevelRequirement(level: Int): IntRange = levelRequirement(level)
    
    /**
     * Checks whether this enchantment is compatible with [other].
     *
     * If [other] is a [VanillaEnchantment], the compatibility function of [other] will be ignored.
     * Otherwise, if [other] is a [NovaEnchantment], both compatibility functions of this and [other] will be taken in account.
     */
    override fun isCompatibleWith(other: Enchantment): Boolean =
        when (other) {
            is VanillaEnchantment -> compatibility(other)
            is NovaEnchantment -> compatibility(other) && other.compatibility(this)
        }
    
    override fun toString() = id.toString()
    
}

internal class VanillaEnchantment(
    override val id: ResourceLocation,
    val enchantment: MojangEnchantment
) : Enchantment {
    
    override val localizedName: String = enchantment.descriptionId
    override val minLevel: Int = enchantment.minLevel
    override val maxLevel: Int = enchantment.maxLevel
    override val rarity: Int = enchantment.rarity.weight
    override val isTableDiscoverable: Boolean = !enchantment.isTreasureOnly && enchantment.isDiscoverable
    override val isTreasure: Boolean = enchantment.isTreasureOnly
    override val isTradeable: Boolean = enchantment.isTradeable
    override val isCurse: Boolean = enchantment.isCurse
    
    override fun getTableLevelRequirement(level: Int): IntRange = enchantment.getMinCost(level)..enchantment.getMaxCost(level)
    
    /**
     * Checks whether this enchantment is compatible with the [other].
     *
     * If [other] is a [VanillaEnchantment], this method will return the result of [MojangEnchantment.isCompatibleWith].
     * Otherwise, if [other] is a [NovaEnchantment], this method will call and return the result of [other.isCompatibleWith][NovaEnchantment.isCompatibleWith].
     */
    override fun isCompatibleWith(other: Enchantment): Boolean =
        when (other) {
            is VanillaEnchantment -> enchantment.isCompatibleWith(other.enchantment)
            is NovaEnchantment -> other.isCompatibleWith(this)
        }
    
    override fun toString() = id.toString()
    
}

class EnchantmentBuilder internal constructor(id: ResourceLocation) : RegistryElementBuilder<Enchantment>(NovaRegistries.ENCHANTMENT, id) {
    
    private var maxLevel: Int = 1
    private var weight: Int = 10
    
    private var tableLeveRequirement: (Int) -> IntRange = { val min = 1 + it * 10; min..(min + 5) }
    private var isTableDiscoverable: Boolean = false
    private var isTreasure: Boolean = false
    private var isTradeable: Boolean = false
    private var isCurse: Boolean = false
    
    private val categories = ArrayList<EnchantmentCategory>()
    private var compatibility: (Enchantment) -> Boolean = { true }
    
    internal constructor(addon: Addon, name: String) : this(ResourceLocation(addon, name))
    
    /**
     * Configures the categories that this enchantment belongs to.
     */
    fun categories(vararg categories: EnchantmentCategory): EnchantmentBuilder {
        this.categories += categories
        return this
    }
    
    /**
     * Configures the level requirement range that specifies whether an enchantment qualifies for a table slot.
     */
    fun tableLevelRequirement(tableLeveRequirement: IntRange): EnchantmentBuilder {
        this.tableLeveRequirement = { tableLeveRequirement }
        return this
    }
    
    /**
     * Configures the level requirement range for a given level that specifies whether an enchantment qualifies for a table slot.
     */
    fun tableLevelRequirement(tableLeveRequirement: (Int) -> IntRange): EnchantmentBuilder {
        this.tableLeveRequirement = tableLeveRequirement
        return this
    }
    
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
    fun tableWeight(weight: Int): EnchantmentBuilder {
        this.weight = weight
        return this
    }
    
    /**
     * Configures the rarity of this enchantment. Defaults to `Rarity.COMMON`.
     */
    fun tableWeight(rarity: Rarity): EnchantmentBuilder {
        this.weight = rarity.weight
        return this
    }
    
    /**
     * Whether this enchantment can appear in the enchanting table. Defaults to `false`.
     */
    fun tableDiscoverable(tableDiscoverable: Boolean): EnchantmentBuilder {
        this.isTableDiscoverable = tableDiscoverable
        return this
    }
    
    /**
     * Whether this enchantment is a treasure enchantment. Defaults to `false`.
     */
    fun treasure(treasure: Boolean): EnchantmentBuilder {
        this.isTreasure = treasure
        return this
    }
    
    /**
     * Whether this enchantment can be traded with villagers. Defaults to `false`.
     */
    fun tradeable(tradeable: Boolean): EnchantmentBuilder {
        this.isTradeable = tradeable
        return this
    }
    
    /**
     * Whether this enchantment is a curse enchantment. Defaults to `false`.
     */
    fun curse(curse: Boolean): EnchantmentBuilder {
        this.isCurse = curse
        return this
    }
    
    /**
     * Sets the compatibility of this enchantment with other enchantments.
     *
     * This option is exclusive with [compatibleWith] and [incompatibleWith].
     */
    fun compatibility(compatibility: (Enchantment) -> Boolean): EnchantmentBuilder {
        this.compatibility = compatibility
        return this
    }
    
    /**
     * Defines with which enchantments this enchantment is compatible. All other enchantments are incompatible.
     *
     * This option is exclusive with [compatibility] and [incompatibleWith].
     */
    fun compatibleWith(vararg enchantments: Enchantment): EnchantmentBuilder {
        this.compatibility = { it in enchantments }
        return this
    }
    
    /**
     * Sets the compatibility of this enchantment with other enchantments.
     *
     * This option is exclusive with [compatibility] and [compatibleWith].
     */
    fun incompatibleWith(vararg enchantments: Enchantment): EnchantmentBuilder {
        this.compatibility = { it !in enchantments }
        return this
    }
    
    /**
     * Builds the enchantment and adds it to the specified categories.
     */
    override fun build(): Enchantment {
        if (isCurse && maxLevel > 1)
            throw IllegalArgumentException("Curse enchantments cannot have multiple levels")
        
        val enchantment = NovaEnchantment(id, 1, maxLevel, weight, isTableDiscoverable, isTreasure, isTradeable, isCurse, tableLeveRequirement, compatibility)
        for (category in categories) category.enchantments += enchantment
        return enchantment
    }
    
}