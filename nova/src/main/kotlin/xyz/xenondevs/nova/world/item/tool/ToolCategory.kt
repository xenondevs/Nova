package xyz.xenondevs.nova.world.item.tool

import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import net.minecraft.core.HolderSet
import net.minecraft.core.component.DataComponents
import net.minecraft.tags.BlockTags
import org.bukkit.block.Block
import org.bukkit.block.BlockType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.registry.entries.BlockTypeTags
import xyz.xenondevs.nova.registry.entries.ItemTypeTags
import xyz.xenondevs.nova.serialization.kotlinx.ToolCategoryEntrySerializer
import xyz.xenondevs.nova.serialization.kotlinx.ToolCategoryEntrySetSerializer
import xyz.xenondevs.nova.serialization.kotlinx.ToolCategorySerializer
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.block.behavior.Breakable
import xyz.xenondevs.nova.world.block.blockType
import xyz.xenondevs.nova.world.block.getBehaviorOrNull
import xyz.xenondevs.nova.world.block.isNova
import xyz.xenondevs.nova.world.item.behavior.Tool
import xyz.xenondevs.nova.world.item.getBehaviorOrNull
import xyz.xenondevs.nova.world.item.itemType

/**
 * Serializable type alias for `RegistryEntry.Nova<ToolCategory>` using [ToolCategoryEntrySerializer].
 */
typealias NovaToolCategoryEntry = @Serializable(with = ToolCategoryEntrySerializer::class) RegistryEntry.Nova<ToolCategory>

/**
 * Serializable type alias for `RegistryEntrySet.Nova<ToolCategory>` using [ToolCategoryEntrySetSerializer].
 */
typealias NovaToolCategoryEntrySet = @Serializable(with = ToolCategoryEntrySetSerializer::class) RegistryEntrySet.Nova<ToolCategory>

/**
 * @param key The [Key] of this [ToolCategory]
 */
@Serializable(with = ToolCategorySerializer::class)
open class ToolCategory internal constructor(
    override val entry: RegistryEntry.Nova<ToolCategory>,
) : NovaRegistryElement<ToolCategory> {
    
    override fun toString(): String = this@ToolCategory.key.asString()
    
    companion object {
        
        fun hasCorrectToolCategory(block: Block, tool: ItemStack?): Boolean {
            val itemToolCategories = ofItem(tool)
            val blockToolCategories = ofBlock(block)
            return itemToolCategories.isNotEmpty() && itemToolCategories.any { it in blockToolCategories }
        }
        
        fun ofItem(item: ItemStack?): Set<ToolCategory> {
            if (item == null)
                return emptySet()
            
            val novaCategory = item.itemType.getBehaviorOrNull<Tool>()?.categories
            if (novaCategory != null)
                return novaCategory
            
            val categories = mutableSetOf<ToolCategory>()
            
            // guess type from tool rules
            val rules = item.unwrap().get(DataComponents.TOOL)?.rules
            if (rules != null) {
                for (rule in rules) {
                    if (!rule.correctForDrops.orElse(false))
                        continue
                    val tagKey = (rule.blocks as? HolderSet.Named<*>)?.key()
                        ?: continue
                    
                    categories += when (tagKey) {
                        BlockTags.MINEABLE_WITH_AXE -> VanillaToolCategories.AXE
                        BlockTags.MINEABLE_WITH_HOE -> VanillaToolCategories.HOE
                        BlockTags.MINEABLE_WITH_PICKAXE -> VanillaToolCategories.PICKAXE
                        BlockTags.MINEABLE_WITH_SHOVEL -> VanillaToolCategories.SHOVEL
                        BlockTags.LEAVES, BlockTags.WOOL -> VanillaToolCategories.SHEARS
                        else -> continue
                    }.get()
                }
            }
            
            // read type from type tags
            val type = item.itemType
            if (type in ItemTypeTags.SHOVELS)
                categories += VanillaToolCategories.SHOVEL.get()
            if (type in ItemTypeTags.PICKAXES)
                categories += VanillaToolCategories.PICKAXE.get()
            if (type in ItemTypeTags.AXES)
                categories += VanillaToolCategories.AXE.get()
            if (type in ItemTypeTags.HOES)
                categories += VanillaToolCategories.HOE.get()
            if (type in ItemTypeTags.SWORDS)
                categories += VanillaToolCategories.SWORD.get()
            if (type == ItemType.SHEARS)
                categories += VanillaToolCategories.SHEARS.get()
            
            return categories
        }
        
        fun ofBlock(block: Block): Set<ToolCategory> {
            val type = block.blockType
            if (type.isNova) 
                return type.getBehaviorOrNull<Breakable>()?.toolCategories ?: emptySet()
            
            val categories = HashSet<ToolCategory>()
            if (type in BlockTypeTags.MINEABLE_SHOVEL)
                categories.add(VanillaToolCategories.SHOVEL.get())
            if (type in BlockTypeTags.MINEABLE_PICKAXE)
                categories.add(VanillaToolCategories.PICKAXE.get())
            if (type in BlockTypeTags.MINEABLE_AXE)
                categories.add(VanillaToolCategories.AXE.get())
            if (type in BlockTypeTags.MINEABLE_HOE)
                categories.add(VanillaToolCategories.HOE.get())
            if (type == BlockType.COBWEB || type == BlockType.BAMBOO_SAPLING || type == BlockType.BAMBOO)
                categories.add(VanillaToolCategories.SWORD.get())
            if (type in BlockTypeTags.LEAVES || type in BlockTypeTags.WOOL || type == BlockType.COBWEB)
                categories.add(VanillaToolCategories.SHEARS.get())
            
            return categories
        }
        
    }
    
}

class VanillaToolCategory internal constructor(
    entry: RegistryEntry.Nova<ToolCategory>,
    val canSweepAttack: Boolean,
    val canBreakBlocksInCreative: Boolean,
    val itemDamageOnAttackEntity: Int,
    val itemDamageOnBreakBlock: Int
) : ToolCategory(entry)
