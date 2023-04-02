package xyz.xenondevs.nova.item.tool

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.item.behavior.Tool
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.pos

/**
 * @param id The [NamespacedId] of this [ToolCategory]
 * @param getIcon Retrieves the icon for this [ToolCategory] with a given [ToolTier]. An example id would
 * be `minecraft:item/diamond_pickaxe` for the `minecraft:pickaxe` category and `minecraft:diamond` level.
 */
open class ToolCategory internal constructor(
    val id: ResourceLocation,
    val getIcon: (ToolTier?) -> ResourcePath
) {
    
    fun isCorrectToolCategoryForBlock(block: Block): Boolean {
        val blockToolCategories = ofBlock(block)
        return this in blockToolCategories
    }
    
    companion object {
        
        fun ofItem(item: ItemStack?): ToolCategory? {
            if (item == null)
                return null
            
            val novaCategory = item.novaItem?.itemLogic?.getBehavior(Tool::class)?.options?.category
            if (novaCategory != null)
                return novaCategory
            
            return when (item.type) {
                Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL, Material.GOLDEN_SHOVEL -> VanillaToolCategories.SHOVEL
                Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE, Material.GOLDEN_PICKAXE -> VanillaToolCategories.PICKAXE
                Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE, Material.GOLDEN_AXE -> VanillaToolCategories.AXE
                Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE, Material.GOLDEN_HOE -> VanillaToolCategories.HOE
                Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.GOLDEN_SWORD -> VanillaToolCategories.SWORD
                Material.SHEARS -> VanillaToolCategories.SHEARS
                
                else -> null
            }
        }
        
        fun ofBlock(block: Block): List<ToolCategory> {
            val novaCategories = BlockManager.getBlockState(block.pos)?.block?.options?.toolCategories
            if (novaCategories != null)
                return novaCategories
            
            return ofVanillaBlock(block)
        }
        
        internal fun ofVanillaBlock(block: Block): List<ToolCategory> {
            val material = block.type
            val list = ArrayList<ToolCategory>()
            if (Tag.MINEABLE_SHOVEL.isTagged(material)) list.add(VanillaToolCategories.SHOVEL)
            if (Tag.MINEABLE_PICKAXE.isTagged(material)) list.add(VanillaToolCategories.PICKAXE)
            if (Tag.MINEABLE_AXE.isTagged(material)) list.add(VanillaToolCategories.AXE)
            if (Tag.MINEABLE_HOE.isTagged(material)) list.add(VanillaToolCategories.HOE)
            if (material == Material.COBWEB || material == Material.BAMBOO) list.add(VanillaToolCategories.SWORD)
            if (Tag.LEAVES.isTagged(material) || Tag.WOOL.isTagged(material) || material == Material.COBWEB) list.add(VanillaToolCategories.SHEARS)
            
            return list
        }
        
    }
    
}

class VanillaToolCategory internal constructor(
    id: ResourceLocation,
    val canSweepAttack: Boolean,
    val canBreakBlocksInCreative: Boolean,
    val itemDamageOnAttackEntity: Int,
    val itemDamageOnBreakBlock: Int,
    val genericMultipliers: Map<Material, Double>,
    val specialMultipliers: Map<Material, Map<Material, Double>>,
    getIcon: (ToolTier?) -> ResourcePath
) : ToolCategory(id, getIcon)