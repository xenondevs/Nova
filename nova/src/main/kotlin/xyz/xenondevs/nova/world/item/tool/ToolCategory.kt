package xyz.xenondevs.nova.world.item.tool

import net.kyori.adventure.key.Key
import net.minecraft.core.HolderSet
import net.minecraft.core.component.DataComponents
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
 * @param id The [Key] of this [ToolCategory]
 */
open class ToolCategory internal constructor(
    val id: Key
) {
    
    override fun toString(): String {
        return id.toString()
    }
    
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
            
            val categories = HashSet<ToolCategory>()
            
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
                    }
                }
            }
            
            // read type from type tags
            val type = item.type
            if (Tag.ITEMS_SHOVELS.isTagged(type))
                categories += VanillaToolCategories.SHOVEL
            if (Tag.ITEMS_PICKAXES.isTagged(type))
                categories += VanillaToolCategories.PICKAXE
            if (Tag.ITEMS_AXES.isTagged(type))
                categories += VanillaToolCategories.AXE
            if (Tag.ITEMS_HOES.isTagged(type))
                categories += VanillaToolCategories.HOE
            if (Tag.ITEMS_SWORDS.isTagged(type))
                categories += VanillaToolCategories.SWORD
            if (type == Material.SHEARS)
                categories += VanillaToolCategories.SHEARS
            
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
    id: Key,
    val canSweepAttack: Boolean,
    val canBreakBlocksInCreative: Boolean,
    val itemDamageOnAttackEntity: Int,
    val itemDamageOnBreakBlock: Int
) : ToolCategory(id)