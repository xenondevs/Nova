package xyz.xenondevs.nova.registry

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.text.Component
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.registries.Registries
import net.minecraft.resources.RegistryOps
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemType
import xyz.xenondevs.nova.util.component.adventure.toNMSComponent
import xyz.xenondevs.nova.util.lookupGetterOrThrow
import xyz.xenondevs.nova.util.toHolderSet
import xyz.xenondevs.nova.world.item.enchantment.CustomEnchantmentLogic
import java.util.*
import net.minecraft.world.item.enchantment.Enchantment as MojangEnchantment

internal class EnchantmentBuilderImpl(
    override val entry: RegistryEntry.Paper<Enchantment>,
) : EnchantmentBuilder, RegistryElementBuilder.Vanilla<MojangEnchantment> {
    
    // enchantment definition
    private var name: Component = Component.translatable("enchantment.${entry.key.namespace()}.${entry.key.value()}")
    private var maxLevel: Int = 1
    private var rarity: Int = 10
    private var anvilCost: Int = 4
    
    // custom logic
    private var tableLeveRequirement: (Int) -> IntRange = { val min = 1 + it * 10; min..(min + 5) }
    private var incompatibleWith: RegistryEntrySet.Paper<Enchantment> = emptyRegistryEntrySet(RegistryKey.ENCHANTMENT)
    private var supportedItems: RegistryEntrySet.Paper<ItemType> = emptyRegistryEntrySet(RegistryKey.ITEM)
    private var primaryItems: RegistryEntrySet.Paper<ItemType>? = null
    
    // tags
    private var isTableDiscoverable: Boolean = false
    private var isTreasure: Boolean = false
    private var isTradeable: Boolean = false
    private var isCurse: Boolean = false
    
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
        this.tableLeveRequirement = { tableLeveRequirement }
    }
    
    override fun tableLevelRequirement(tableLeveRequirement: (level: Int) -> IntRange) {
        this.tableLeveRequirement = tableLeveRequirement
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
        
        CustomEnchantmentLogic.customEnchantments[enchantment] = CustomEnchantmentLogic(tableLeveRequirement)
        
        return enchantment
    }
    
    override fun buildTagSet(): Set<TagKey<*>> = buildSet {
        if (isTableDiscoverable)
            add(EnchantmentTagKeys.IN_ENCHANTING_TABLE)
        if (isCurse)
            add(EnchantmentTagKeys.CURSE)
        if (isTradeable)
            add(EnchantmentTagKeys.TRADEABLE)
        if (isTreasure)
            add(EnchantmentTagKeys.TREASURE)
    }
    
}