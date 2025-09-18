package xyz.xenondevs.nova.resources.builder.layout.gui

import org.joml.Vector2i
import org.joml.Vector2ic

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
     * Aligns the gui texture to the top left corner of the menu, using [baseOffset] based on the
     * menu type and [extraOffset] for an additional custom offset.
     */
    class TopLeft(
        private val baseOffset: Vector2ic = CHEST_OFFSET,
        private val extraOffset: Vector2ic = Vector2i(0, 0)
    ) : GuiTextureAlignment {
        
        constructor(baseOffset: Vector2ic = CHEST_OFFSET, extraX: Int = 0, extraY: Int = 0) : this(
            baseOffset,
            Vector2i(extraX, extraY)
        )
        
        override fun getOffset(width: Int, height: Int): Vector2ic {
            return Vector2i(baseOffset).add(extraOffset)
        }
        
    }
    
    /**
     * A horizontally centered alignment for gui textures, using [baseOffset] based on the
     * menu type and [yOffset] for an additional vertical offset.
     */
    class HorizontallyCentered(
        private val baseOffset: Vector2ic = CHEST_OFFSET,
        private val baseWidth: Int = CHEST_WIDTH,
        private val yOffset: Int = 0
    ) : GuiTextureAlignment {
        
        override fun getOffset(width: Int, height: Int): Vector2ic {
            return Vector2i((baseWidth - width) / 2, yOffset).add(baseOffset)
        }
        
    }
    
    companion object {
        
        /**
         * The width of the anvil menu.
         */
        const val ANVIL_WIDTH: Int = 176
        
        /**
         * The width of the blast furnace menu.
         */
        const val BLAST_FURNACE_WIDTH: Int = 176
        
        /**
         * The width of the brewing stand menu.
         */
        const val BREWING_STAND_WIDTH: Int = 176
        
        /**
         * The width of the cartography table menu.
         */
        const val CARTOGRAPHY_TABLE_WIDTH: Int = 176
        
        /**
         * The width of the chest menu.
         */
        const val CHEST_WIDTH: Int = 176
        
        /**
         * The width of the crafter menu.
         */
        const val CRAFTER_WIDTH: Int = 176
        
        /**
         * The width of the crafting table menu.
         */
        const val CRAFTING_TABLE_WIDTH: Int = 176
        
        /**
         * The width of the dispenser menu.
         */
        const val DISPENSER_WIDTH: Int = 176
        
        /**
         * The width of the dropper menu.
         */
        const val DROPPER_WIDTH: Int = 176
        
        /**
         * The width of the enchantment table menu.
         */
        const val ENCHANTMENT_TABLE_WIDTH: Int = 176
        
        /**
         * The width of the furnace menu.
         */
        const val FURNACE_WIDTH: Int = 176
        
        /**
         * The width of the grindstone menu.
         */
        const val GRINDSTONE_WIDTH: Int = 176
        
        /**
         * The width of the hopper menu.
         */
        const val HOPPER_WIDTH: Int = 176
        
        /**
         * The width of the loom menu.
         */
        const val LOOM_WIDTH: Int = 176
        
        /**
         * The width of the shulker box menu.
         */
        const val SHULKER_BOX_WIDTH: Int = 176
        
        /**
         * The width of the smithing table menu.
         */
        const val SMITHING_TABLE_WIDTH: Int = 176
        
        /**
         * The width of the smoker menu.
         */
        const val SMOKER_WIDTH: Int = 176
        
        /**
         * The width of the stonecutter menu.
         */
        const val STONECUTTER_WIDTH: Int = 176
        
        /**
         * The default offset that is required to perfectly overlap the anvil gui texture.
         */
        val ANVIL_OFFSET: Vector2ic = Vector2i(-60, -13)
        
        /**
         * The default offset that is required to perfectly overlap the blast furnace gui texture.
         */
        val BLAST_FURNACE_OFFSET: Vector2ic = Vector2i(-88, -13)
        
        /**
         * The default offset that is required to perfectly overlap the brewing stand gui texture.
         */
        val BREWING_STAND_OFFSET: Vector2ic = Vector2i(-88, -13)
        
        /**
         * The default offset that is required to perfectly overlap the cartography table gui texture.
         */
        val CARTOGRAPHY_TABLE_OFFSET: Vector2ic = Vector2i(-8, -11)
        
        /**
         * The default offset that is required to perfectly overlap the chest (generic) gui texture.
         */
        val CHEST_OFFSET: Vector2ic = Vector2i(-8, -13)
        
        /**
         * The default offset that is required to perfectly overlap the crafter gui texture.
         */
        val CRAFTER_OFFSET: Vector2ic = Vector2i(-88, -13)
        
        /**
         * The default offset that is required to perfectly overlap the crafting table gui texture.
         */
        val CRAFTING_TABLE_OFFSET: Vector2ic = Vector2i(-29, -13)
        
        /**
         * The default offset that is required to perfectly overlap the dispenser gui texture.
         */
        val DISPENSER_OFFSET: Vector2ic = Vector2i(-88, -13)
        
        /**
         * The default offset that is required to perfectly overlap the dropper gui texture.
         */
        val DROPPER_OFFSET: Vector2ic = Vector2i(-88, -13)
        
        /**
         * The default offset that is required to perfectly overlap the enchantment table gui texture.
         */
        val ENCHANTMENT_TABLE_OFFSET: Vector2ic = Vector2i(-8, -13)
        
        /**
         * The default offset that is required to perfectly overlap the furnace gui texture.
         */
        val FURNACE_OFFSET: Vector2ic = Vector2i(-88, -13)
        
        /**
         * The default offset that is required to perfectly overlap the grindstone gui texture.
         */
        val GRINDSTONE_OFFSET: Vector2ic = Vector2i(-8, -13)
        
        /**
         * The default offset that is required to perfectly overlap the hopper gui texture.
         */
        val HOPPER_OFFSET: Vector2ic = Vector2i(-8, -13)
        
        /**
         * The default offset that is required to perfectly overlap the loom gui texture.
         */
        val LOOM_OFFSET: Vector2ic = Vector2i(-8, -11)
        
        /**
         * The default offset that is required to perfectly overlap the shulker box gui texture.
         */
        val SHULKER_BOX_OFFSET: Vector2ic = Vector2i(-8, -13)
        
        /**
         * The default offset that is required to perfectly overlap the smithing table gui texture.
         */
        val SMITHING_TABLE_OFFSET: Vector2ic = Vector2i(-44, -22)
        
        /**
         * The default offset that is required to perfectly overlap the smoker gui texture.
         */
        val SMOKER_OFFSET: Vector2ic = Vector2i(-88, -13)
        
        /**
         * The default offset that is required to perfectly overlap the stonecutter gui texture.
         */
        val STONECUTTER_OFFSET: Vector2ic = Vector2i(-8, -12)
        
    }
    
}