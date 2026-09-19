package xyz.xenondevs.nova.registry

import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.text.Component
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.registries.Registries
import net.minecraft.resources.RegistryOps
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemType
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.uninitializedProvider
import xyz.xenondevs.nova.registry.entries.EnchantmentTags
import xyz.xenondevs.nova.util.component.adventure.toNMSComponent
import xyz.xenondevs.nova.util.lookupGetterOrThrow
import xyz.xenondevs.nova.util.toHolderSet
import xyz.xenondevs.nova.world.item.enchantment.CustomEnchantmentLogic
import java.util.Optional
import net.minecraft.world.item.enchantment.Enchantment as MojangEnchantment

internal class EnchantmentBuilderImpl(
    override val entry: RegistryEntry.Paper<Enchantment>,
) : EnchantmentBuilder, RegistryElementBuilder.RerunnableVanilla<Enchantment, MojangEnchantment> {
    
    private val _name = uninitializedProvider<Component>()
    private val _maxLevel = uninitializedProvider<Int>()
    private val _rarity = uninitializedProvider<Int>()
    private val _anvilCost = uninitializedProvider<Int>()
    private val _tableLevelRequirement = uninitializedProvider<(Int) -> IntRange>()
    private val _incompatibleWith = uninitializedProvider<RegistryEntrySet.Paper<Enchantment>>()
    private val _supportedItems = uninitializedProvider<RegistryEntrySet.Paper<ItemType>>()
    private val _primaryItems = uninitializedProvider<RegistryEntrySet.Paper<ItemType>?>()
    private val _isTableDiscoverable = uninitializedProvider<Boolean>()
    private val _isTreasure = uninitializedProvider<Boolean>()
    private val _isTradeable = uninitializedProvider<Boolean>()
    private val _isCurse = uninitializedProvider<Boolean>()
    
    private var name: Component by _name
    private var maxLevel: Int by _maxLevel
    private var rarity: Int by _rarity
    private var anvilCost: Int by _anvilCost
    private var tableLevelRequirement: (Int) -> IntRange by _tableLevelRequirement
    private var incompatibleWith: RegistryEntrySet.Paper<Enchantment> by _incompatibleWith
    private var supportedItems: RegistryEntrySet.Paper<ItemType> by _supportedItems
    private var primaryItems: RegistryEntrySet.Paper<ItemType>? by _primaryItems
    private var isTableDiscoverable: Boolean by _isTableDiscoverable
    private var isTreasure: Boolean by _isTreasure
    private var isTradeable: Boolean by _isTradeable
    private var isCurse: Boolean by _isCurse
    
    override val tags = combinedProvider(
        _isTableDiscoverable, _isTreasure, _isTradeable, _isCurse
    ) { isTableDiscoverable, isTreasure, isTradeable, isCurse ->
        buildSet {
            if (isTableDiscoverable)
                add(EnchantmentTags.IN_ENCHANTING_TABLE)
            if (isCurse)
                add(EnchantmentTags.CURSE)
            if (isTradeable)
                add(EnchantmentTags.TRADEABLE)
            if (isTreasure)
                add(EnchantmentTags.TREASURE)
        }
    }
    
    override fun reset() {
        name = Component.translatable("enchantment.${entry.key.namespace()}.${entry.key.value()}")
        maxLevel = 1
        rarity = 10
        anvilCost = 4
        tableLevelRequirement = { level ->
            val min = 1 + level * 10
            min..(min + 5)
        }
        incompatibleWith = emptyRegistryEntrySet(RegistryKey.ENCHANTMENT)
        supportedItems = emptyRegistryEntrySet(RegistryKey.ITEM)
        primaryItems = null
        isTableDiscoverable = false
        isTreasure = false
        isTradeable = false
        isCurse = false
    }
    
    override fun name(name: Component) {
        this.name = name
    }
    
    override fun maxLevel(maxLevel: Int) {
        require(!isCurse) { "Curse enchantments cannot have multiple levels" }
        this.maxLevel = maxLevel
    }
    
    override fun anvilCost(anvilCost: Int) {
        this.anvilCost = anvilCost
    }
    
    override fun tableLevelRequirement(tableLeveRequirement: IntRange) {
        this.tableLevelRequirement = { tableLeveRequirement }
    }
    
    override fun tableLevelRequirement(tableLeveRequirement: (level: Int) -> IntRange) {
        this.tableLevelRequirement = tableLeveRequirement
    }
    
    override fun rarity(weight: Int) {
        require(weight in 1..1024) { "Rarity must be between 1 and 1024" }
        this.rarity = weight
    }
    
    override fun tableDiscoverable(tableDiscoverable: Boolean) {
        this.isTableDiscoverable = tableDiscoverable
    }
    
    override fun treasure(treasure: Boolean) {
        this.isTreasure = treasure
    }
    
    override fun tradeable(tradeable: Boolean) {
        this.isTradeable = tradeable
    }
    
    override fun curse(curse: Boolean) {
        require(maxLevel == 1) { "Curse enchantments cannot have multiple levels" }
        this.isCurse = curse
    }
    
    override fun enchantsPrimary(items: RegistryEntrySet.Paper<ItemType>) {
        primaryItems = items
    }
    
    override fun enchants(items: RegistryEntrySet.Paper<ItemType>) {
        supportedItems = items
    }
    
    override fun incompatibleWith(enchantments: RegistryEntrySet.Paper<Enchantment>) {
        incompatibleWith = enchantments
    }
    
    override fun build(lookup: RegistryOps.RegistryInfoLookup): MojangEnchantment {
        val itemRegistry = lookup.lookupGetterOrThrow(Registries.ITEM)
        val enchRegistry = lookup.lookupGetterOrThrow(Registries.ENCHANTMENT)
        
        val enchantment = MojangEnchantment(
            name.toNMSComponent(),
            MojangEnchantment.EnchantmentDefinition(
                supportedItems.toHolderSet(itemRegistry),
                Optional.ofNullable(primaryItems?.toHolderSet(itemRegistry)),
                rarity,
                maxLevel,
                MojangEnchantment.Cost(0, 0),
                MojangEnchantment.Cost(0, 0),
                anvilCost,
                emptyList()
            ),
            incompatibleWith.toHolderSet(enchRegistry),
            DataComponentMap.EMPTY
        )
        
        CustomEnchantmentLogic.customEnchantments[enchantment] = CustomEnchantmentLogic(_tableLevelRequirement)
        
        return enchantment
    }
    
}