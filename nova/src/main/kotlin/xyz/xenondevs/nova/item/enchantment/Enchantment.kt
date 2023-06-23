package xyz.xenondevs.nova.item.enchantment

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.RegistryElementBuilder
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.getOrThrow
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
     * Returns the minimum exp cost for enchanting an item with this enchantment at the given level.
     */
    fun getMinCost(level: Int): Int
    
    /**
     * Returns the maximum exp cost for enchanting an item with this enchantment at the given level.
     */
    fun getMaxCost(level: Int): Int
    
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
        
    }
    
}

internal class NovaEnchantment(
    override val id: ResourceLocation,
    override val minLevel: Int,
    override val maxLevel: Int,
    private val minCost: (Int) -> Int,
    private val maxCost: (Int) -> Int,
    private val compatibility: (Enchantment) -> Boolean
) : Enchantment {
    
    override val localizedName = "enchantment.${id.namespace}.${id.path}"
    
    override fun getMinCost(level: Int): Int = minCost(level)
    override fun getMaxCost(level: Int): Int = maxCost(level)
    
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
    
}

internal class VanillaEnchantment(
    override val id: ResourceLocation,
    private val enchantment: MojangEnchantment
) : Enchantment {
    
    override val localizedName: String = enchantment.descriptionId
    override val minLevel: Int = enchantment.minLevel
    override val maxLevel: Int = enchantment.maxLevel
    
    override fun getMinCost(level: Int): Int = enchantment.getMinCost(level)
    override fun getMaxCost(level: Int): Int = enchantment.getMaxCost(level)
    
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
    
}

class EnchantmentBuilder internal constructor(id: ResourceLocation) : RegistryElementBuilder<Enchantment>(NovaRegistries.ENCHANTMENT, id) {
    
    private var minLevel: Int = 1
    private var maxLevel: Int = 1
    
    private var minCost: (Int) -> Int = { 1 + it * 10 }
    private var maxCost: (Int) -> Int = { (1 + it * 10) + 5 }
    
    private val categories = ArrayList<EnchantmentCategory>()
    private var compatibility: (Enchantment) -> Boolean = { true }
    
    internal constructor(addon: Addon, name: String) : this(ResourceLocation(addon, name))
    
    fun categories(vararg categories: EnchantmentCategory): EnchantmentBuilder {
        this.categories += categories
        return this
    }
    
    /**
     * Sets the minimum level of this enchantment. Defaults to 1.
     */
    fun minLevel(minLevel: Int): EnchantmentBuilder {
        this.minLevel = minLevel
        return this
    }
    
    /**
     * Sets the maximum level of this enchantment. Defaults to 1.
     */
    fun maxLevel(maxLevel: Int): EnchantmentBuilder {
        this.maxLevel = maxLevel
        return this
    }
    
    /**
     * Sets the minimum exp cost of this enchantment. Defaults to ``1 + level * 10``.
     */
    fun minCost(minCost: Int): EnchantmentBuilder {
        this.minCost = { minCost }
        return this
    }
    
    /**
     * Sets the minimum exp cost of this enchantment, based on the level. Defaults to ``1 + level * 10``.
     */
    fun minCost(minCost: (Int) -> Int): EnchantmentBuilder {
        this.minCost = minCost
        return this
    }
    
    /**
     * Sets the maximum exp cost of this enchantment. Defaults to ``(1 + level * 10) + 5``.
     */
    fun maxCost(maxCost: Int): EnchantmentBuilder {
        this.maxCost = { maxCost }
        return this
    }
    
    /**
     * Sets the maximum exp cost of this enchantment, based on the level. Defaults to ``(1 + level * 10) + 5``.
     */
    fun maxCost(maxCost: (Int) -> Int): EnchantmentBuilder {
        this.maxCost = maxCost
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
        val enchantment = NovaEnchantment(
            id,
            minLevel, maxLevel,
            minCost, maxCost,
            compatibility
        )
        
        for (category in categories) {
            category.enchantments += enchantment
        }
        
        return enchantment
    }
    
}