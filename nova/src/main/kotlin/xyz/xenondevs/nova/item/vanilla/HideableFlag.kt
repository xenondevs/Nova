package xyz.xenondevs.nova.item.vanilla

enum class HideableFlag {
    
    /**
     * Hides enchantments.
     */
    ENCHANTMENTS,
    
    /**
     * Hides attribute modifiers like damage or attack speed.
     */
    MODIFIERS,
    
    /**
     * Hides the unbreakable state.
     */
    UNBREAKABLE,
    
    /**
     * Hides which blocks an item can destroy.
     */
    CAN_DESTROY,
    
    /**
     * Hides where this block can be placed on.
     */
    CAN_PLACE,
    
    /**
     * Hides additional information like potion effects, book and firework information, map tooltips,
     * patterns of banners, and enchantments of enchanted books.
     */
    ADDITIONAL,
    
    /**
     * Hides leather armor dye color.
     */
    DYE;
    
    companion object {
        
        fun toInt(flags: Iterable<HideableFlag>): Int {
            var i = 0
            flags.forEach { i = i or (1 shl it.ordinal) }
            return i
        }
        
    }
    
}