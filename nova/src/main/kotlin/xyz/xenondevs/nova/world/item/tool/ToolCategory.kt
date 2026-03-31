package xyz.xenondevs.nova.world.item.tool

import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import net.minecraft.core.HolderSet
import net.minecraft.core.component.DataComponents
import net.minecraft.tags.BlockTags
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.serialization.kotlinx.ToolCategoryEntrySerializer
import xyz.xenondevs.nova.serialization.kotlinx.ToolCategoryEntrySetSerializer
import xyz.xenondevs.nova.serialization.kotlinx.ToolCategorySerializer
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.block.behavior.Breakable
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.world.item.behavior.Tool
import xyz.xenondevs.nova.world.pos

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
    
    override fun toString(): String = key.toString()
    
    companion object {
        
        fun hasCorrectToolCategory(block: Block, tool: ItemStack?): Boolean {
            val itemToolCategories = ofItem(tool)
            val blockToolCategories = ofBlock(block)
            return itemToolCategories.isNotEmpty() && itemToolCategories.any { it in blockToolCategories }
        }
        
        fun ofItem(item: ItemStack?): Set<ToolCategory> {
            if (item == null)
                return emptySet()
            
            val novaCategory = item.novaItem?.getBehaviorOrNull(Tool::class)?.categories
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
            val type = item.type
            if (Tag.ITEMS_SHOVELS.isTagged(type))
                categories += VanillaToolCategories.SHOVEL.get()
            if (Tag.ITEMS_PICKAXES.isTagged(type))
                categories += VanillaToolCategories.PICKAXE.get()
            if (Tag.ITEMS_AXES.isTagged(type))
                categories += VanillaToolCategories.AXE.get()
            if (Tag.ITEMS_HOES.isTagged(type))
                categories += VanillaToolCategories.HOE.get()
            if (Tag.ITEMS_SWORDS.isTagged(type))
                categories += VanillaToolCategories.SWORD.get()
            if (type == Material.SHEARS)
                categories += VanillaToolCategories.SHEARS.get()
            
            return categories
        }
        
        fun ofBlock(block: Block): Set<ToolCategory> {
            val novaBlock = WorldDataManager.getBlockState(block.pos)?.block
            if (novaBlock != null) {
                val breakable = novaBlock.getBehaviorOrNull<Breakable>()
                return breakable?.toolCategories ?: emptySet()
            }
            
            val type = block.type
            val categories = HashSet<ToolCategory>()
            if (Tag.MINEABLE_SHOVEL.isTagged(type))
                categories.add(VanillaToolCategories.SHOVEL.get())
            if (Tag.MINEABLE_PICKAXE.isTagged(type))
                categories.add(VanillaToolCategories.PICKAXE.get())
            if (Tag.MINEABLE_AXE.isTagged(type))
                categories.add(VanillaToolCategories.AXE.get())
            if (Tag.MINEABLE_HOE.isTagged(type))
                categories.add(VanillaToolCategories.HOE.get())
            if (type == Material.COBWEB || type == Material.BAMBOO_SAPLING || type == Material.BAMBOO)
                categories.add(VanillaToolCategories.SWORD.get())
            if (Tag.LEAVES.isTagged(type) || Tag.WOOL.isTagged(type) || type == Material.COBWEB)
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