@file:Suppress("FunctionName")

package xyz.xenondevs.nova.world.item.behavior

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Tool.*
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.tag.TagKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import net.kyori.adventure.util.TriState
import org.bukkit.block.BlockType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.mapEachTo
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.registry.alias.BlockTypeEntrySet
import xyz.xenondevs.nova.registry.entries.BlockTypeEntries
import xyz.xenondevs.nova.registry.registryEntrySetOf
import xyz.xenondevs.nova.registry.tags.BlockTypeTags
import xyz.xenondevs.nova.serialization.kotlinx.KeySerializer
import xyz.xenondevs.nova.serialization.kotlinx.ValueOrListSerializer
import xyz.xenondevs.nova.util.pluralize
import xyz.xenondevs.nova.world.item.buildDataComponentMapProvider

/**
 * Creates a factory for [Tool] behaviors using the given values, if not specified otherwise in the item's config.
 *
 * @param rules The ordered rules defining special mining behavior for matching blocks.
 * Each rule matches [Rule.blocks] and may override [Rule.speed] and [Rule.correctForDrops].
 * For each value, the first matching rule that specifies it wins. Speed falls back to
 * [defaultBreakSpeed], while correctness falls back to `false`. Configured rules use the
 * `blocks`, optional `speed`, and optional `correct_for_drops` fields.
 * Defaults to an empty list.
 * Used when `rules` is not specified in the item's config.
 *
 * @param defaultBreakSpeed The mining speed used when no matching rule overrides it.
 * Defaults to `1.0`.
 * Used when neither `default_break_speed` nor `default_mining_speed` is specified in the item's config.
 *
 * @param itemDamageOnBreakBlock The durability removed whenever a block is broken. Must be
 * non-negative.
 * Defaults to `1`.
 * Used when neither `item_damage_on_break_block` nor `damage_per_block` is specified in the item's config.
 *
 * @param canBreakBlocksInCreative Whether players can break blocks in Creative mode while holding
 * this tool.
 * Defaults to `true`.
 * Used when neither `can_break_blocks_in_creative` nor `can_destroy_blocks_in_creative` is specified in the item's config.
 */
fun Tool(
    rules: List<Tool.Rule>,
    defaultBreakSpeed: Float = 1f,
    itemDamageOnBreakBlock: Int = 1,
    canBreakBlocksInCreative: Boolean = true
) = ItemBehaviorFactory { _, cfg ->
    Tool(
        cfg.entry<List<Tool.Rule>>(rules, "rules"),
        cfg.entry(defaultBreakSpeed, ["default_break_speed"], ["default_mining_speed"]), // vanilla name "default_mining_speed" as fallback
        cfg.entry(itemDamageOnBreakBlock, ["item_damage_on_break_block"], ["damage_per_block"]), // vanilla name "damage_per_block" as fallback
        cfg.entry(canBreakBlocksInCreative, ["can_break_blocks_in_creative"], ["can_destroy_blocks_in_creative"]) // vanilla name "destroy" as fallback
    )
}

private val SWORD_CATEGORY = Key.key("sword")
private val SHEARS_CATEGORY = Key.key("shears")

private data class ResolvedToolCategory(
    val key: Key,
    val itemTag: RegistryEntrySet.Paper.Tag<ItemType>,
    val mineableTag: RegistryEntrySet.Paper.Tag<BlockType>?
)

/**
 * Creates a factory for [Tool] behaviors using the given values, if not specified otherwise in the item's config.
 * 
 * This factory uses the legacy concepts of a [toolTier] and [toolCategories] to infer [tool rules][Tool.Rule].
 * Note that for custom tool tiers and categories, the relevant tags must be registered.
 * For full flexibility, use the [Tool] factory that accepts [tool rules][Tool.Rule] directly.
 * 
 * Both `tool_tier` and `tool_categories` can be overridden in the item's config by defining `rules` directly.
 *
 * @param toolTier The tier of the tool.
 * Valid options are any key where a corresponding tag `<namespace>:incorrect_for_<value>_tool` exists in the block registry.
 * (ex: `minecraft:wooden`, `minecraft:diamond`, etc.)
 * Defaults to `minecraft:wooden`.
 * Used when `tool_tier` or `tool_level` is not specified in the item's config.
 * 
 * @param toolCategories The categories of the tool.
 * Valid options are any keys where a corresponding tag `<namespace>:mineable/<value>` and a pluralized tag `<namespace>:<values>` exist in the block and item registries, respectively.
 * (ex: `minecraft:pickaxe`, --> block tag `minecraft:mineable/pickaxe` and item tag `minecraft:pickaxes`)
 * Defaults to `[minecraft:pickaxe]`.
 * Used when `tool_categories` is not specified in the item's config.
 * 
 * @param breakSpeed The mining speed to use when the tool matches. Does not apply to swords or shears, as those have hardcoded mining speeds.
 * Defaults to `1f`.
 * Used when `break_speed` is not specified in the item's config.
 *
 * @param itemDamageOnBreakBlock The durability removed whenever a block is broken.
 * Defaults to `1`.
 * Used when neither `item_damage_on_break_block` nor `damage_per_block` is specified in the item's config.
 *
 * @param canBreakBlocksInCreative Whether players can break blocks in creative mode while holding this tool.
 * Defaults to `true`.
 * Used when neither `can_break_blocks_in_creative` nor `can_destroy_blocks_in_creative` is specified in the item's config.
 */
fun Tool(
    toolTier: Key = Key.key("wooden"),
    toolCategories: Set<Key> = setOf(Key.key("pickaxe")),
    breakSpeed: Float = 1f,
    itemDamageOnBreakBlock: Int = 1,
    canBreakBlocksInCreative: Boolean = true
) = ItemBehaviorFactory { _, cfg ->
    fun getIncorrectForToolTag(toolTier: Key): RegistryEntrySet.Paper.Tag<BlockType> {
        val tierName = when (toolTier) {
            Key.key("wood") -> "wooden"
            else -> toolTier.value()
        }
        val tagName = Key.key(toolTier.namespace(), "incorrect_for_${tierName}_tool")
        return registryEntrySetOf(TagKey.create(RegistryKey.BLOCK, tagName))
    }
    
    fun getMineableTag(category: Key): RegistryEntrySet.Paper.Tag<BlockType> {
        val tagName = Key.key(category.namespace(), "mineable/${category.value()}")
        return registryEntrySetOf(TagKey.create(RegistryKey.BLOCK, tagName))
    }
    
    fun getItemTag(category: Key): RegistryEntrySet.Paper.Tag<ItemType> {
        val tagName = Key.key(category.namespace(), category.value().pluralize())
        return registryEntrySetOf(TagKey.create(RegistryKey.ITEM, tagName))
    }
    
    fun resolveToolCategory(category: Key) = ResolvedToolCategory(
        category,
        getItemTag(category),
        if (category != SWORD_CATEGORY && category != SHEARS_CATEGORY) getMineableTag(category) else null
    )
    
    val toolTier = cfg.entry(
        KeySerializer,
        toolTier,
        listOf("tool_tier"),
        listOf("tool_level")
    ) { getIncorrectForToolTag(it) }
    
    val toolCategories = cfg.entry(
        ValueOrListSerializer(KeySerializer),
        toolCategories.toList(),
        "tool_category"
    ) { categories ->
        categories.map(::resolveToolCategory)
    }
    
    val breakSpeed = cfg.entry(breakSpeed, "break_speed")   
    
    val inferredRules = combinedProvider(
        toolTier, toolCategories, breakSpeed
    ) { toolTier, toolCategories, breakSpeed ->
        val isSword = toolCategories.any { it.key == SWORD_CATEGORY }
        val isShears = toolCategories.any { it.key == SHEARS_CATEGORY }
        val regularCategories = toolCategories.mapNotNull { it.mineableTag }
        
        buildList {
            if (regularCategories.isNotEmpty()) {
                val incorrectTag = registryEntrySetOf(toolTier.tagKey)
                add(Tool.Rule(incorrectTag, false, null))
                
                for (tag in regularCategories) {
                    val mineableTag = registryEntrySetOf(tag.tagKey)
                    add(Tool.Rule(mineableTag, true, breakSpeed))
                }
            }
            
            if (isSword) {
                add(Tool.Rule(registryEntrySetOf(BlockTypeEntries.COBWEB), true, 15f))
                add(Tool.Rule(BlockTypeTags.SWORD_INSTANTLY_MINES, null, Float.MAX_VALUE))
                add(Tool.Rule(BlockTypeTags.SWORD_EFFICIENT, null, 1.5f))
            }
            
            if (isShears) {
                add(Tool.Rule(registryEntrySetOf(BlockTypeEntries.COBWEB), true, 15f))
                add(Tool.Rule(BlockTypeTags.SHEARS_EXTREME_BREAKING_SPEED, null, 15f))
                add(Tool.Rule(BlockTypeTags.SHEARS_MAJOR_BREAKING_SPEED, null, 5f))
                add(Tool.Rule(BlockTypeTags.SHEARS_MINOR_BREAKING_SPEED, null, 2f))
            }
        }
    }
    
    Tool(
        rules = cfg.entry(inferredRules, "rules"),
        itemTags = toolCategories.mapEachTo(HashSet<*>::newHashSet) { it.itemTag },
        defaultBreakSpeed = cfg.entry(1f, ["default_break_speed"], ["default_mining_speed"]),
        itemDamageOnBreakBlock = cfg.entry(itemDamageOnBreakBlock, ["item_damage_on_break_block"], ["damage_per_block"]), // vanilla name "damage_per_block" as fallback
        canBreakBlocksInCreative = cfg.entry(canBreakBlocksInCreative, ["can_break_blocks_in_creative"], ["can_destroy_blocks_in_creative"]) // vanilla name "destroy" as fallback
    )
}

/**
 * Allows items to mine blocks using configurable mining rules.
 *
 * @param rules The ordered rules defining special mining behavior for matching blocks.
 * For each value, the first matching rule that specifies it wins. Speed falls back to
 * [defaultBreakSpeed], while correctness falls back to `false`.
 *
 * @param defaultBreakSpeed The mining speed used when no matching rule overrides it.
 * @param itemDamageOnBreakBlock The durability removed whenever a block is broken.
 * @param canBreakBlocksInCreative Whether players can break blocks in Creative mode while holding
 * this tool.
 */
class Tool internal constructor(
    rules: Provider<List<Rule>>,
    defaultBreakSpeed: Provider<Float>,
    itemDamageOnBreakBlock: Provider<Int>,
    canBreakBlocksInCreative: Provider<Boolean>,
    itemTags: Provider<Set<RegistryEntrySet.Paper.Tag<ItemType>>>
) : ItemBehavior {
    
    constructor(
        rules: Provider<List<Rule>>,
        defaultBreakSpeed: Provider<Float>,
        itemDamageOnBreakBlock: Provider<Int>,
        canBreakBlocksInCreative: Provider<Boolean>
    ) : this(rules, defaultBreakSpeed, itemDamageOnBreakBlock, canBreakBlocksInCreative, provider(emptySet()))
    
    /**
     * The ordered rules defining special mining behavior for matching blocks.
     *
     * Each rule may override mining speed, correctness for drops, or both. If multiple matching
     * rules override the same property, the first one takes precedence.
     */
    val rules: List<Rule> by rules
    
    /**
     * The mining speed used when no matching [rule][rules] overrides it.
     */
    val defaultBreakSpeed: Float by defaultBreakSpeed
    
    /**
     * The amount of damage to the item stack per broken block.
     */
    val itemDamageOnBreakBlock: Int by itemDamageOnBreakBlock
    
    /**
     * Whether this tool can be used to break blocks in creative mode.
     */
    val canBreakBlocksInCreative: Boolean by canBreakBlocksInCreative
    
    override val tags = itemTags
    
    override val baseDataComponents = buildDataComponentMapProvider {
        this[DataComponentTypes.TOOL] = combinedProvider(
            defaultBreakSpeed, itemDamageOnBreakBlock, canBreakBlocksInCreative, rules
        ) { defaultBreakSpeed, itemDamageOnBreakBlock, canBreakBlocksInCreative, rules ->
            tool()
                .defaultMiningSpeed(defaultBreakSpeed)
                .damagePerBlock(itemDamageOnBreakBlock)
                .canDestroyBlocksInCreative(canBreakBlocksInCreative)
                .addRules(rules.map { (blocks, correctForDrops, speed) -> rule(blocks.toRegistryKeySet(), speed, TriState.byBoolean(correctForDrops)) })
                .build()
        }
    }
    
    override fun toString(itemStack: ItemStack): String {
        return "Tool(" +
            "rules=$rules, " +
            "defaultBreakSpeed=$defaultBreakSpeed, " +
            "itemDamageOnBreakBlock=$itemDamageOnBreakBlock, " +
            "canBreakBlocksInCreative=$canBreakBlocksInCreative" +
            ")"
    }
    
    /**
     * A [tool rule](https://minecraft.wiki/w/Data_component_format#tool).
     */
    @Serializable
    data class Rule(
        /**
         * The blocks that this rule applies to.
         */
        val blocks: BlockTypeEntrySet,
        /**
         * Whether this tool is correct for drops when [blocks] matched.
         */
        @SerialName("correct_for_drops")
        val correctForDrops: Boolean? = null,
        /**
         * The mining speed to use when [blocks] matched.
         */
        val speed: Float? = null
    )
    
}