package xyz.xenondevs.nova.item.tool

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.item.behavior.Tool
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.pos

/**
 * @param id The [NamespacedId] of this [ToolCategory]
 * @param getMultiplier Retrieves the multiplier for a specific [ItemStack] of this [ToolCategory]
 * @param getIcon Retrieves the icon for this [ToolCategory] with a given [ToolLevel]. An example id would 
 * be `minecraft:item/diamond_pickaxe` for the `minecraft:pickaxe` category and `minecraft:diamond` level.
 */
class ToolCategory internal constructor(
    val id: NamespacedId,
    val getMultiplier: (ItemStack) -> Double,
    val getIcon: (ToolLevel?) -> ResourcePath
) {
    
    fun isCorrectToolCategoryForBlock(block: Block): Boolean {
        val blockToolCategories = ofBlock(block)
        return this in blockToolCategories
    }
    
    companion object {
        
        val SHOVEL = ToolCategoryRegistry.register(
            "shovel",
            mapOf(
                Material.WOODEN_SHOVEL to 2.0,
                Material.STONE_SHOVEL to 4.0,
                Material.IRON_SHOVEL to 6.0,
                Material.DIAMOND_SHOVEL to 8.0,
                Material.NETHERITE_SHOVEL to 9.0,
                Material.GOLDEN_SHOVEL to 12.0
            )
        )
        
        val PICKAXE = ToolCategoryRegistry.register(
            "pickaxe",
            mapOf(
                Material.WOODEN_PICKAXE to 2.0,
                Material.STONE_PICKAXE to 4.0,
                Material.IRON_PICKAXE to 6.0,
                Material.DIAMOND_PICKAXE to 8.0,
                Material.NETHERITE_PICKAXE to 9.0,
                Material.GOLDEN_PICKAXE to 12.0
            )
        )
        
        val AXE = ToolCategoryRegistry.register(
            "axe",
            mapOf(
                Material.WOODEN_AXE to 2.0,
                Material.STONE_AXE to 4.0,
                Material.IRON_AXE to 6.0,
                Material.DIAMOND_AXE to 8.0,
                Material.NETHERITE_AXE to 9.0,
                Material.GOLDEN_AXE to 12.0
            )
        )
        
        val HOE = ToolCategoryRegistry.register(
            "hoe",
            mapOf(
                Material.WOODEN_HOE to 2.0,
                Material.STONE_HOE to 4.0,
                Material.IRON_HOE to 6.0,
                Material.DIAMOND_HOE to 8.0,
                Material.NETHERITE_HOE to 9.0,
                Material.GOLDEN_HOE to 12.0
            )
        )
        
        val SWORD = ToolCategoryRegistry.register(
            "sword",
            mapOf(
                Material.WOODEN_SWORD to 1.5,
                Material.STONE_SWORD to 1.5,
                Material.IRON_SWORD to 1.5,
                Material.DIAMOND_SWORD to 1.5,
                Material.NETHERITE_SWORD to 1.5,
                Material.GOLDEN_SWORD to 1.5
            )
        )
        
        val SHEARS = ToolCategoryRegistry.register(
            "shears",
            mapOf(
                Material.SHEARS to 1.5
            )
        ) { ResourcePath.of("minecraft:item/shears") }
        
        fun ofItem(item: ItemStack): ToolCategory? {
            val novaCategory = item.novaMaterial?.novaItem?.getBehavior(Tool::class)?.options?.category
            if (novaCategory != null)
                return novaCategory
            
            return when (item.type) {
                Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL, Material.GOLDEN_SHOVEL -> SHOVEL
                Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE, Material.GOLDEN_PICKAXE -> PICKAXE
                Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE, Material.GOLDEN_AXE -> AXE
                Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE, Material.GOLDEN_HOE -> HOE
                Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.GOLDEN_SWORD -> SWORD
                Material.SHEARS -> SHEARS
                
                else -> null
            }
        }
        
        fun ofBlock(block: Block): List<ToolCategory> {
            val novaCategories = BlockManager.getBlock(block.pos)?.material?.toolCategories
            if (novaCategories != null)
                return novaCategories
            
            val material = block.type
            val list = ArrayList<ToolCategory>()
            if (Tag.MINEABLE_SHOVEL.isTagged(material)) list.add(SHOVEL)
            if (Tag.MINEABLE_PICKAXE.isTagged(material)) list.add(PICKAXE)
            if (Tag.MINEABLE_AXE.isTagged(material)) list.add(AXE)
            if (Tag.MINEABLE_HOE.isTagged(material)) list.add(HOE)
            if (material == Material.COBWEB || material == Material.BAMBOO) list.add(SWORD)
            if (Tag.LEAVES.isTagged(material) || Tag.WOOL.isTagged(material) || material == Material.COBWEB) list.add(SHEARS)
            
            return list
        }
        
    }
    
}