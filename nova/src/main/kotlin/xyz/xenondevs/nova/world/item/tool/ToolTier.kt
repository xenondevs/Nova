@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.world.item.tool

import kotlinx.serialization.Serializable
import net.minecraft.core.HolderSet
import net.minecraft.core.component.DataComponents
import net.minecraft.tags.BlockTags
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.registry.bootstrapFlatMap
import xyz.xenondevs.nova.registry.entries.BlockTypeTags
import xyz.xenondevs.nova.serialization.kotlinx.ToolTierEntrySerializer
import xyz.xenondevs.nova.serialization.kotlinx.ToolTierEntrySetSerializer
import xyz.xenondevs.nova.serialization.kotlinx.ToolTierSerializer
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.block.behavior.Breakable
import xyz.xenondevs.nova.world.block.blockType
import xyz.xenondevs.nova.world.block.getBehaviorOrNull
import xyz.xenondevs.nova.world.block.isNova
import xyz.xenondevs.nova.world.item.behavior.Tool
import xyz.xenondevs.nova.world.item.getBehaviorOrNull
import xyz.xenondevs.nova.world.item.itemType

/**
 * Serializable type alias for `RegistryEntry.Nova<ToolTier>` using [ToolTierEntrySerializer].
 */
typealias NovaToolTierEntry = @Serializable(with = ToolTierEntrySerializer::class) RegistryEntry.Nova<ToolTier>

/**
 * Serializable type alias for `RegistryEntrySet.Nova<ToolTier>` using [ToolTierEntrySetSerializer].
 */
typealias NovaToolTierEntrySet = @Serializable(with = ToolTierEntrySetSerializer::class) RegistryEntrySet.Nova<ToolTier>

/**
 * Shortcut to [bootstrapFlatMap][bootstrapFlatMap] to [ToolTier.levelValue].
 */
val Provider<ToolTier>.levelValue: Provider<Double>
    get() = bootstrapFlatMap { it.levelValue }

@Serializable(with = ToolTierSerializer::class)
class ToolTier(
    override val entry: RegistryEntry.Nova<ToolTier>,
    val levelValue: Provider<Double>
) : Comparable<ToolTier>, NovaRegistryElement<ToolTier> {
    
    override fun compareTo(other: ToolTier): Int {
        val levelCompare = levelValue.get().compareTo(other.levelValue.get())
        if (levelCompare != 0)
            return levelCompare
        return this@ToolTier.key.compareTo(other.key)
    }
    
    override fun toString(): String = this@ToolTier.key.asString()
    
    companion object {
        
        /**
         * Returns the [ToolTier] of the given [Block].
         * This method works for both vanilla and Nova blocks.
         */
        fun ofBlock(block: Block): ToolTier {
            val type = block.blockType
            if (type.isNova)
                return type.getBehaviorOrNull<Breakable>()?.toolTier ?: VanillaToolTiers.WOOD.get()
            
            return when (type) {
                in BlockTypeTags.NEEDS_STONE_TOOL -> VanillaToolTiers.STONE
                in BlockTypeTags.NEEDS_IRON_TOOL -> VanillaToolTiers.IRON
                in BlockTypeTags.NEEDS_DIAMOND_TOOL -> VanillaToolTiers.DIAMOND
                else -> VanillaToolTiers.WOOD
            }.get()
        }
        
        /**
         * Returns the [ToolTier] of the given [ItemStack].
         * This method works for both vanilla and Nova items.
         * If the provided [ItemStack] is not a tool, null will be returned.
         */
        fun ofItem(item: ItemStack?): ToolTier? {
            if (item == null)
                return null
            
            val novaLevel = item.itemType.getBehaviorOrNull<Tool>()?.tier
            if (novaLevel != null)
                return novaLevel
            
            val rules = item.unwrap().get(DataComponents.TOOL)?.rules
                ?: return null
            
            val tiers = HashSet<ToolTier>()
            for (rule in rules) {
                val correctForDrops = rule.correctForDrops
                if (correctForDrops.isEmpty)
                    continue
                
                val key = (rule.blocks as? HolderSet.Named<*>)?.key()
                    ?: continue
                
                if (correctForDrops.get()) {
                    tiers += when (key) {
                        BlockTags.NEEDS_DIAMOND_TOOL -> VanillaToolTiers.DIAMOND
                        BlockTags.NEEDS_IRON_TOOL -> VanillaToolTiers.IRON
                        BlockTags.NEEDS_STONE_TOOL -> VanillaToolTiers.STONE
                        else -> continue
                    }.get()
                } else {
                    tiers += when (key) {
                        BlockTags.INCORRECT_FOR_NETHERITE_TOOL -> VanillaToolTiers.NETHERITE
                        BlockTags.INCORRECT_FOR_DIAMOND_TOOL -> VanillaToolTiers.DIAMOND
                        BlockTags.INCORRECT_FOR_GOLD_TOOL -> VanillaToolTiers.GOLD
                        BlockTags.INCORRECT_FOR_IRON_TOOL -> VanillaToolTiers.IRON
                        BlockTags.INCORRECT_FOR_COPPER_TOOL -> VanillaToolTiers.COPPER
                        BlockTags.INCORRECT_FOR_STONE_TOOL -> VanillaToolTiers.STONE
                        BlockTags.INCORRECT_FOR_WOODEN_TOOL -> VanillaToolTiers.WOOD
                        else -> continue
                    }.get()
                }
            }
            
            return tiers.max()
        }
        
        fun isCorrectLevel(block: Block, tool: ItemStack?): Boolean {
            val toolLevel = ofItem(tool)
            val blockLevel = ofBlock(block)
            return isCorrectLevel(blockLevel, toolLevel)
        }
        
        fun isCorrectLevel(blockTier: ToolTier?, toolTier: ToolTier?): Boolean {
            return isCorrectLevel(blockTier?.levelValue?.get(), toolTier?.levelValue?.get())
        }
        
        fun isCorrectLevel(blockLevel: Double?, toolLevel: Double?): Boolean {
            return blockLevel == null || (toolLevel ?: 0.0) >= blockLevel
        }
        
    }
    
}
