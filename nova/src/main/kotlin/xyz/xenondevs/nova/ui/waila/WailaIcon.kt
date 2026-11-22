package xyz.xenondevs.nova.ui.waila

import org.bukkit.block.data.BlockData
import org.bukkit.inventory.ItemType
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType

/**
 * The icon that is displayed in the WAILA overlay for a block.
 *
 * Icons are rendered or copied into the resource pack during resource pack build.
 *
 * @see WailaIconProvider
 */
sealed interface WailaIcon {
    
    /**
     * Renders the model of [itemType].
     */
    data class Item(
        val itemType: ItemType,
        val framing: Framing = Gui
    ) : WailaIcon
    
    /**
     * Renders the block model of [blockState].
     */
    data class BlockState(
        val blockState: BlockData,
        val framing: Framing = Gui
    ) : WailaIcon
    
    /**
     * Renders the model at [path].
     */
    data class Model(
        val path: ResourcePath<ResourceType.Model>,
        val framing: Framing = Gui
    ) : WailaIcon
    
    /**
     * Uses the texture at [path] as-is, without rendering.
     */
    data class Texture(
        val path: ResourcePath<ResourceType.Texture>
    ) : WailaIcon
    
    /**
     * Describes how a rendered model is framed in the icon.
     */
    sealed interface Framing {
        
        /**
         * Framed like the model is displayed in an inventory slot.
         */
        data object Gui : Framing
        
        /**
         * Zoomed and centered so that the model's geometry spans the icon,
         * leaving [margin] (a fraction of the icon size, less than `0.5`) free on each side.
         */
        data class Fit(val margin: Float) : Framing {
            
            init {
                require(margin in 0f..<0.5f) { "Margin must be in 0..<0.5" }
            }
            
        }
        
    }
    
}