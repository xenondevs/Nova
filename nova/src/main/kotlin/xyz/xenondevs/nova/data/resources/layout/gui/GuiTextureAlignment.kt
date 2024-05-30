package xyz.xenondevs.nova.data.resources.layout.gui

import org.joml.Vector2i
import org.joml.Vector2ic

/**
 * The width of the chest gui.
 */
private const val CHEST_GUI_WIDTH: Int = 176

/**
 * The default offset that is required to perfectly overlap the chest gui texture.
 */
private val DEFAULT_CHEST_OFFSET: Vector2ic = Vector2i(-8, -13)

/**
 * The default offset that is required to perfectly overlap the anvil gui texture.
 */
private val DEFAULT_ANVIL_OFFSET: Vector2ic = Vector2i(-60, -13)

/**
 * Generates offsets for gui textures.
 */
interface GuiTextureAlignment {
    
    /**
     * Returns the offset based on the [width] and [height] of the gui texture.
     */
    fun getOffset(width: Int, height: Int): Vector2ic
    
    /**
     * A custom alignment with a fixed [offset].
     */
    class Custom(private val offset: Vector2ic) : GuiTextureAlignment {
        
        override fun getOffset(width: Int, height: Int): Vector2ic {
            return offset
        }
        
    }
    
    /**
     * The default (top-left) alignment for chest gui textures.
     */
    object ChestDefault : GuiTextureAlignment {
        
        override fun getOffset(width: Int, height: Int): Vector2ic {
            return DEFAULT_CHEST_OFFSET
        }
        
    }
    
    /**
     * The default (top-left) alignment for anvil gui textures.
     */
    object AnvilDefault : GuiTextureAlignment {
        
        override fun getOffset(width: Int, height: Int): Vector2ic {
            return DEFAULT_ANVIL_OFFSET
        }
        
    }
    
    /**
     * A horizontally centered alignment for chest gui textures.
     */
    class ChestHorizontallyCentered(private val yOffset: Int = 0) : GuiTextureAlignment {
        
        override fun getOffset(width: Int, height: Int): Vector2ic {
            return Vector2i((CHEST_GUI_WIDTH - width) / 2, yOffset).add(DEFAULT_CHEST_OFFSET)
        }
        
    }
    
}