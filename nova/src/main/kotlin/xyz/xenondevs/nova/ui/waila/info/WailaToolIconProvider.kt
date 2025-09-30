package xyz.xenondevs.nova.ui.waila.info

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.resources.builder.task.TextureIconContent
import xyz.xenondevs.nova.world.item.tool.ToolCategory
import xyz.xenondevs.nova.world.item.tool.ToolTier
import xyz.xenondevs.nova.world.item.tool.VanillaToolCategories.AXE
import xyz.xenondevs.nova.world.item.tool.VanillaToolCategories.HOE
import xyz.xenondevs.nova.world.item.tool.VanillaToolCategories.PICKAXE
import xyz.xenondevs.nova.world.item.tool.VanillaToolCategories.SHEARS
import xyz.xenondevs.nova.world.item.tool.VanillaToolCategories.SHOVEL
import xyz.xenondevs.nova.world.item.tool.VanillaToolCategories.SWORD
import xyz.xenondevs.nova.world.item.tool.VanillaToolTiers
import xyz.xenondevs.nova.world.item.tool.VanillaToolTiers.COPPER
import xyz.xenondevs.nova.world.item.tool.VanillaToolTiers.DIAMOND
import xyz.xenondevs.nova.world.item.tool.VanillaToolTiers.GOLD
import xyz.xenondevs.nova.world.item.tool.VanillaToolTiers.IRON
import xyz.xenondevs.nova.world.item.tool.VanillaToolTiers.NETHERITE
import xyz.xenondevs.nova.world.item.tool.VanillaToolTiers.STONE
import xyz.xenondevs.nova.world.item.tool.VanillaToolTiers.WOOD

/**
 * Chooses tool icons for WAILA.
 */
interface WailaToolIconProvider {
    
    /**
     * Returns a [Key] for the location of the texture to be used for tools of the given [category] and [tier].
     * Note that textures which are intended to be used for these icons need to be added to the texture icon font via [TextureIconContent.addIcons]
     * using a custom resource pack task.
     */
    fun getIcon(category: ToolCategory, tier: ToolTier?): Key?
    
}

internal object VanillaWailaToolIconProvider : WailaToolIconProvider {
    
    override fun getIcon(category: ToolCategory, tier: ToolTier?): Key? {
        val name = when (category) {
            SHEARS -> "shears"
            SHOVEL, PICKAXE, AXE, HOE, SWORD -> when (tier) {
                WOOD, GOLD -> "${tier.id.value()}en_${category.id.value()}"
                STONE, COPPER, IRON, DIAMOND, NETHERITE -> "${tier.id.value()}_${category.id.value()}"
                else -> null
            }
            
            else -> null
        } ?: return null
        
        return Key.key("item/$name")
    }
    
}