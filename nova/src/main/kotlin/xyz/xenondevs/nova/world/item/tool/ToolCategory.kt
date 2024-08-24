package xyz.xenondevs.nova.world.item.tool

import net.minecraft.core.HolderSet
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.BlockTags
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.block.behavior.Breakable
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.world.item.behavior.Tool
import xyz.xenondevs.nova.world.pos

/**
 * @param id The [ResourceLocation] of this [ToolCategory]
 */
open class ToolCategory internal constructor(
    val id: ResourceLocation
) {
    
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
            
            val rules = item.unwrap().get(DataComponents.TOOL)?.rules
                ?: return emptySet()
            
            val categories = HashSet<ToolCategory>()
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
                }
            }
            
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
                categories.add(VanillaToolCategories.SHOVEL)
            if (Tag.MINEABLE_PICKAXE.isTagged(type))
                categories.add(VanillaToolCategories.PICKAXE)
            if (Tag.MINEABLE_AXE.isTagged(type))
                categories.add(VanillaToolCategories.AXE)
            if (Tag.MINEABLE_HOE.isTagged(type))
                categories.add(VanillaToolCategories.HOE)
            if (type == Material.COBWEB || type == Material.BAMBOO_SAPLING || type == Material.BAMBOO)
                categories.add(VanillaToolCategories.SWORD)
            if (Tag.LEAVES.isTagged(type) || Tag.WOOL.isTagged(type) || type == Material.COBWEB)
                categories.add(VanillaToolCategories.SHEARS)
            
            return categories
        }
        
    }
    
}

class VanillaToolCategory internal constructor(
    id: ResourceLocation,
    val canSweepAttack: Boolean,
    val canBreakBlocksInCreative: Boolean,
    val itemDamageOnAttackEntity: Int,
    val itemDamageOnBreakBlock: Int
) : ToolCategory(id)