package xyz.xenondevs.nova.world.item.enchantment

import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.minecraft.core.HolderSet
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.registries.Registries
import net.minecraft.resources.RegistryOps
import net.minecraft.tags.EnchantmentTags
import net.minecraft.world.item.enchantment.Enchantment
import org.bukkit.craftbukkit.enchantments.CraftEnchantment
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.registry.plusAssign
import xyz.xenondevs.nova.registry.LazyRegistryElementBuilder
import xyz.xenondevs.nova.util.component.adventure.toNMSComponent
import xyz.xenondevs.nova.world.item.behavior.Enchantable
import java.util.*
import net.minecraft.world.item.enchantment.Enchantment as MojangEnchantment
import org.bukkit.enchantments.Enchantment as BukkitEnchantment

internal class CustomEnchantmentLogic(
    private val primaryItem: (ItemStack) -> Boolean,
    private val supportedItem: (ItemStack) -> Boolean,
    private val tableLevelRequirement: (Int) -> IntRange,
    private val compatibility: (MojangEnchantment) -> Boolean
) {
    
    fun isPrimaryItem(itemStack: ItemStack): Boolean =
        primaryItem(itemStack)
    
    fun isSupportedItem(itemStack: ItemStack): Boolean =
        supportedItem(itemStack)
    
    fun compatibleWith(other: MojangEnchantment): Boolean =
        compatibility(other)
    
    fun getMinCost(level: Int): Int =
        tableLevelRequirement(level).first
    
    fun getMaxCost(level: Int): Int =
        tableLevelRequirement(level).last
    
    companion object {
        
        @JvmField
        val customEnchantments = IdentityHashMap<Enchantment, CustomEnchantmentLogic>()
        
    }
    
}

class EnchantmentBuilder internal constructor(
    id: Key,
) : LazyRegistryElementBuilder<BukkitEnchantment, MojangEnchantment>(
    Registries.ENCHANTMENT,
    CraftEnchantment::minecraftHolderToBukkit,
    id
) {
    
    // enchantment definition
    private var name: Component = Component.translatable("enchantment.${id.namespace()}.${id.value()}")
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
    fun compatibility(compatibility: (BukkitEnchantment) -> Boolean) {
        this.compatibility = { compatibility(CraftEnchantment.minecraftToBukkit(it)) }
    }
    
    /**
     * Defines with which enchantments this enchantment is compatible. All other enchantments are incompatible.
     *
     * This option is exclusive with [compatibility] and [incompatibleWith].
     */
    @Suppress("UnstableApiUsage")
    fun compatibleWith(vararg enchantments: TypedKey<BukkitEnchantment>) {
        val keySet = enchantments.mapTo(HashSet()) { it.key() }
        this.compatibility = { CraftEnchantment.minecraftToBukkit(it).key() in keySet }
    }
    
    /**
     * Sets the compatibility of this enchantment with other enchantments.
     *
     * This option is exclusive with [compatibility] and [compatibleWith].
     */
    @Suppress("UnstableApiUsage")
    fun incompatibleWith(vararg enchantments: TypedKey<BukkitEnchantment>) {
        val keySet = enchantments.mapTo(HashSet()) { it.key() }
        this.compatibility = { CraftEnchantment.minecraftToBukkit(it).key() !in keySet }
    }
    
    /**
     * Builds the enchantment and adds it to the specified categories.
     */
    override fun build(lookup: RegistryOps.RegistryInfoLookup): MojangEnchantment {
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
        
        CustomEnchantmentLogic.customEnchantments[enchantment] = CustomEnchantmentLogic(
            primaryItem, supportedItem, tableLeveRequirement, compatibility
        )
        
        return enchantment
    }
    
    override fun register(): Provider<BukkitEnchantment> {
        if (isTableDiscoverable)
            EnchantmentTags.IN_ENCHANTING_TABLE += id
        if (isCurse)
            EnchantmentTags.CURSE += id
        if (isTradeable)
            EnchantmentTags.TRADEABLE += id
        if (isTreasure)
            EnchantmentTags.TREASURE += id
        
        UnknownEnchantments.rememberEnchantmentId(id)
        
        return super.register()
    }
    
}