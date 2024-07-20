package xyz.xenondevs.nova.item.enchantment

import net.kyori.adventure.text.Component
import net.minecraft.core.HolderSet
import net.minecraft.core.component.DataComponentMap
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.EnchantmentTags
import net.minecraft.world.item.enchantment.Enchantment
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.item.behavior.Enchantable
import xyz.xenondevs.nova.registry.RegistryElementBuilder
import xyz.xenondevs.nova.registry.vanilla.VanillaRegistries
import xyz.xenondevs.nova.transformer.patch.item.EnchantmentPatches
import xyz.xenondevs.nova.transformer.patch.registry.TagsPatch
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.component.adventure.toNMSComponent
import xyz.xenondevs.nova.util.name
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.HOLDER_REFERENCE_BIND_VALUE_METHOD
import xyz.xenondevs.nova.util.register
import java.util.*
import net.minecraft.world.item.enchantment.Enchantment as MojangEnchantment

internal class CustomEnchantmentLogic(
    private val primaryItem: (ItemStack) -> Boolean,
    private val supportedItem: (ItemStack) -> Boolean,
    private val tableLevelRequirement: (Int) -> IntRange,
    private val compatability: (MojangEnchantment) -> Boolean
) {
    
    fun isPrimaryItem(itemStack: ItemStack): Boolean =
        primaryItem(itemStack)
    
    fun isSupportedItem(itemStack: ItemStack): Boolean =
        supportedItem(itemStack)
    
    fun compatibleWith(other: MojangEnchantment): Boolean =
        compatability(other)
    
    fun getMinCost(level: Int): Int =
        tableLevelRequirement(level).first
    
    fun getMaxCost(level: Int): Int =
        tableLevelRequirement(level).last
    
}

class EnchantmentBuilder internal constructor(id: ResourceLocation) : RegistryElementBuilder<MojangEnchantment>(VanillaRegistries.ENCHANTMENT, id) {
    
    // enchantment definition
    private var name: Component = Component.translatable("enchantment.${id.namespace}.${id.name}")
    private var maxLevel: Int = 1
    private var rarity: Int = 10
    private var anvilCost: Int = 4
    
    // custom logic
    private var tableLeveRequirement: (Int) -> IntRange = { val min = 1 + it * 10; min..(min + 5) }
    private var compatibility: (MojangEnchantment) -> Boolean = { true }
    private var primaryItem: (ItemStack) -> Boolean = { false }
    private var supportedItem: (ItemStack) -> Boolean = { false }
    
    // tags
    private var isTableDiscoverable: Boolean = false
    private var isTreasure: Boolean = false
    private var isTradeable: Boolean = false
    private var isCurse: Boolean = false
    
    internal constructor(addon: Addon, name: String) : this(ResourceLocation(addon, name))
    
    /**
     * Sets the name of the enchantment.
     *
     * This function is exclusive with [localizedName].
     */
    fun name(name: Component) {
        this.name = name
    }
    
    /**
     * Sets the localization key of the enchantment.
     *
     * Defaults to `enchantment.<namespace>.<name>`.
     */
    fun localizedName(localizedName: String) {
        this.name = Component.translatable(localizedName)
    }
    
    /**
     * Configures the maximum level of this enchantment. Defaults to `1`.
     */
    fun maxLevel(maxLevel: Int) {
        this.maxLevel = maxLevel
    }
    
    /**
     * Configures the cost of this enchantment in an anvil. Defaults to `4`.
     */
    fun anvilCost(anvilCost: Int) {
        this.anvilCost = anvilCost
    }
    
    /**
     * Configures the level range where the enchantment can appear in an enchanting table slot.
     */
    fun tableLevelRequirement(tableLeveRequirement: IntRange) {
        this.tableLeveRequirement = { tableLeveRequirement }
    }
    
    /**
     * Configures the level range where the enchantment can appear in an enchanting table slot, based on the enchantment level.
     */
    fun tableLevelRequirement(tableLeveRequirement: (level: Int) -> IntRange) {
        this.tableLeveRequirement = tableLeveRequirement
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
    fun rarity(weight: Int) {
        require(weight in 1..1024) { "Rarity must be between 1 and 1024" }
        this.rarity = weight
    }
    
    /**
     * Whether this enchantment can appear in the enchanting table. Defaults to `false`.
     */
    fun tableDiscoverable(tableDiscoverable: Boolean) {
        this.isTableDiscoverable = tableDiscoverable
    }
    
    /**
     * Whether this enchantment is a treasure enchantment. Defaults to `false`.
     */
    fun treasure(treasure: Boolean) {
        this.isTreasure = treasure
    }
    
    /**
     * Whether this enchantment can be traded with villagers. Defaults to `false`.
     */
    fun tradeable(tradeable: Boolean) {
        this.isTradeable = tradeable
    }
    
    /**
     * Whether this enchantment is a curse enchantment. Defaults to `false`.
     */
    fun curse(curse: Boolean) {
        this.isCurse = curse
    }
    
    /**
     * Sets the items for which this enchantment can show up in the enchanting table.
     *
     * For your own custom items, prefer configuring the supported enchantments via
     * the [Enchantable] item behavior.
     */
    fun enchantsPrimary(canEnchant: (ItemStack) -> Boolean) {
        primaryItem = canEnchant
    }
    
    /**
     * Sets the items to which this enchantment can be applied to, for example in an anvil.
     *
     * To have the enchantment appear in the enchanting table, use [enchantsPrimary].
     *
     * For your own custom items, prefer configuring the supported enchantments via
     * the [Enchantable] item behavior.
     */
    fun enchants(canEnchant: (ItemStack) -> Boolean) {
        supportedItem = canEnchant
    }
    
    /**
     * Sets the compatibility of this enchantment with other enchantments.
     *
     * This option is exclusive with [compatibleWith] and [incompatibleWith].
     */
    fun compatibility(compatibility: (MojangEnchantment) -> Boolean) {
        this.compatibility = compatibility
    }
    
    /**
     * Defines with which enchantments this enchantment is compatible. All other enchantments are incompatible.
     *
     * This option is exclusive with [compatibility] and [incompatibleWith].
     */
    fun compatibleWith(vararg enchantments: MojangEnchantment) {
        this.compatibility = { it in enchantments }
    }
    
    /**
     * Sets the compatibility of this enchantment with other enchantments.
     *
     * This option is exclusive with [compatibility] and [compatibleWith].
     */
    fun incompatibleWith(vararg enchantments: MojangEnchantment) {
        this.compatibility = { it !in enchantments }
    }
    
    /**
     * Builds the enchantment and adds it to the specified categories.
     */
    override fun build(): MojangEnchantment {
        if (isCurse && maxLevel > 1)
            throw IllegalArgumentException("Curse enchantments cannot have multiple levels")
        
        val enchantment = MojangEnchantment(
            name.toNMSComponent(),
            MojangEnchantment.EnchantmentDefinition(
                HolderSet.direct(),
                Optional.empty(),
                rarity,
                maxLevel,
                MojangEnchantment.Cost(0, 0),
                MojangEnchantment.Cost(0, 0),
                anvilCost,
                emptyList()
            ),
            HolderSet.direct(),
            DataComponentMap.EMPTY
        )
        
        return enchantment
    }
    
    override fun register(): Enchantment {
        val enchantment = build()
        val holder = registry.register(id, enchantment)
        HOLDER_REFERENCE_BIND_VALUE_METHOD.invoke(holder, enchantment)
        
        EnchantmentPatches.customEnchantments[enchantment] = CustomEnchantmentLogic(
            primaryItem, supportedItem, tableLeveRequirement, compatibility
        )
        
        if (isTableDiscoverable)
            TagsPatch.addExtra(EnchantmentTags.IN_ENCHANTING_TABLE, holder)
        if (isCurse)
            TagsPatch.addExtra(EnchantmentTags.CURSE, holder)
        if (isTradeable)
            TagsPatch.addExtra(EnchantmentTags.TRADEABLE, holder)
        if (isTreasure)
            TagsPatch.addExtra(EnchantmentTags.TREASURE, holder)
        
        UnknownEnchantments.rememberEnchantmentId(id)
        
        return enchantment
    }
    
}