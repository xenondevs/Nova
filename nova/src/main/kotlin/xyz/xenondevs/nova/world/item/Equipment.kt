package xyz.xenondevs.nova.world.item

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.layout.equipment.EquipmentLayout

/**
 * Represents a custom armor texture.
 */
class Equipment internal constructor(
    val id: Key,
    internal val makeLayout: (ResourcePackBuilder) -> EquipmentLayout
) {
    
    companion object {
        
        val LEATHER = of(Key.key("minecraft", "leather"))
        val CHAINMAIL = of(Key.key("minecraft", "chainmail"))
        val IRON = of(Key.key("minecraft", "iron"))
        val GOLD = of(Key.key("minecraft", "gold"))
        val DIAMOND = of(Key.key("minecraft", "diamond"))
        val NETHERITE = of(Key.key("minecraft", "netherite"))
        val ELYTRA = of(Key.key("minecraft", "elytra"))
        
        val ARMADILLO_SCUTE = of(Key.key("minecraft", "armadillo_scute"))
        val TURTLE_SCUTE = of(Key.key("minecraft", "turtle_scute"))
        val TRADER_LLAMA = of(Key.key("minecraft", "trader_llama"))
        
        val BLACK_CARPET = of(Key.key("minecraft", "black_carpet"))
        val BLUE_CARPET = of(Key.key("minecraft", "blue_carpet"))
        val BROWN_CARPET = of(Key.key("minecraft", "brown_carpet"))
        val CYAN_CARPET = of(Key.key("minecraft", "cyan_carpet"))
        val GRAY_CARPET = of(Key.key("minecraft", "gray_carpet"))
        val GREEN_CARPET = of(Key.key("minecraft", "green_carpet"))
        val LIGHT_BLUE_CARPET = of(Key.key("minecraft", "light_blue_carpet"))
        val LIGHT_GRAY_CARPET = of(Key.key("minecraft", "light_gray_carpet"))
        val LIME_CARPET = of(Key.key("minecraft", "lime_carpet"))
        val MAGENTA_CARPET = of(Key.key("minecraft", "magenta_carpet"))
        val ORANGE_CARPET = of(Key.key("minecraft", "orange_carpet"))
        val PINK_CARPET = of(Key.key("minecraft", "pink_carpet"))
        val PURPLE_CARPET = of(Key.key("minecraft", "purple_carpet"))
        val RED_CARPET = of(Key.key("minecraft", "red_carpet"))
        val WHITE_CARPET = of(Key.key("minecraft", "white_carpet"))
        val YELLOW_CARPET = of(Key.key("minecraft", "yellow_carpet"))
        
        /**
         * Creates a new [Equipment] with the specified [key], assuming
         * that an equipment under that id already exists.
         */
        fun of(key: Key): Equipment =
            Equipment(key) { throw UnsupportedOperationException() }
        
    }
    
}