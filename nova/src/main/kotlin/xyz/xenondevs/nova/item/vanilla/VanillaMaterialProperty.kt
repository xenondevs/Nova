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
    CONSUMABLE_ALWAYS,
    
    /**
     * The item can render a custom helmet texture.
     */
    HELMET,
    
    /**
     * The item can render a custom chestplate texture.
     */
    CHESTPLATE,
    
    /**
     * The item can render a custom leggings texture.
     */
    LEGGINGS,
    
    /**
     * The item can render a custom boots texture.
     */
    BOOTS
    
}