package xyz.xenondevs.nova.item.vanilla

enum class HideableFlag {
    
    ENCHANTMENTS,
    MODIFIERS,
    UNBREAKABLE,
    CAN_DESTROY,
    CAN_PLACE;
    
    companion object {
        
        fun toInt(flags: Iterable<HideableFlag>): Int {
            var i = 0
            flags.forEach { i = i or (1 shl it.ordinal) }
            return i
        }
        
    }
    
}