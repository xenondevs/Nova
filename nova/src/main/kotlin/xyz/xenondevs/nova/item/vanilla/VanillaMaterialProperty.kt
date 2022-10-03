package xyz.xenondevs.nova.item.vanilla

enum class VanillaMaterialProperty {
    
    /**
     * The item has a durability bar.
     */
    DAMAGEABLE,
    
    /**
     * The item can't break any blocks in creative.
     */
    CREATIVE_NON_BLOCK_BREAKING,
    
    /**
     * The item can be consumed normally.
     */
    CONSUMABLE_NORMAL,
    
    /**
     * The item can be consumed fast.
     */
    CONSUMABLE_FAST,
    
    /**
     * The item can always be consumed.
     */
    CONSUMABLE_ALWAYS
    
}