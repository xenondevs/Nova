package xyz.xenondevs.nova.ui.waila.info

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.ToolTier
import xyz.xenondevs.nova.item.tool.VanillaToolCategories.AXE
import xyz.xenondevs.nova.item.tool.VanillaToolCategories.HOE
import xyz.xenondevs.nova.item.tool.VanillaToolCategories.PICKAXE
import xyz.xenondevs.nova.item.tool.VanillaToolCategories.SHEARS
import xyz.xenondevs.nova.item.tool.VanillaToolCategories.SHOVEL
import xyz.xenondevs.nova.item.tool.VanillaToolCategories.SWORD
import xyz.xenondevs.nova.item.tool.VanillaToolTiers.DIAMOND
import xyz.xenondevs.nova.item.tool.VanillaToolTiers.GOLD
import xyz.xenondevs.nova.item.tool.VanillaToolTiers.IRON
import xyz.xenondevs.nova.item.tool.VanillaToolTiers.NETHERITE
import xyz.xenondevs.nova.item.tool.VanillaToolTiers.STONE
import xyz.xenondevs.nova.item.tool.VanillaToolTiers.WOOD
import xyz.xenondevs.nova.util.name

interface WailaToolIconProvider {
    
    fun getIcon(category: ToolCategory, tier: ToolTier?): ResourceLocation?
    
}

internal object VanillaWailaToolIconProvider : WailaToolIconProvider {
    
    override fun getIcon(category: ToolCategory, tier: ToolTier?): ResourceLocation? {
        val name = when (category) {
            SHEARS -> "shears"
            SHOVEL, PICKAXE, AXE, HOE, SWORD -> when(tier) {
                WOOD, GOLD -> "${tier.id.name}en_${category.id.name}"
                STONE, IRON, DIAMOND, NETHERITE -> "${tier.id.name}_${category.id.name}"
                else -> "wooden_${category.id.name}"
            }
            else -> null
        } ?: return null
        
        return ResourceLocation("minecraft", "item/$name")
    }
    
}