package xyz.xenondevs.nova.item.vanilla

enum class VanillaMaterialProperty(internal val importance: Int) {
    
    /**
     * The item has a durability bar.
     */
    DAMAGEABLE(2),
    
    /**
     * The item can't break any blocks in creative.
     */
    CREATIVE_NON_BLOCK_BREAKING(1),
    
    /**
     * The item can be consumed normally.
     */
    CONSUMABLE_NORMAL(3),
    
    /**
     * The item can be consumed fast.
     */
    CONSUMABLE_FAST(3),
    
    /**
     * The item can always be consumed.
     */
    CONSUMABLE_ALWAYS(3),
    
    /**
     * The item can render a custom helmet texture.
     */
    HELMET(3),
    
    /**
     * The item can render a custom chestplate texture.
     */
    CHESTPLATE(3),
    
    /**
     * The item can render a custom leggings texture.
     */
    LEGGINGS(3),
    
    /**
     * The item can render a custom boots texture.
     */
    BOOTS(3),
    
    /**
     * The item will not catch on fire.
     */
    FIRE_RESISTANT(0)
    
}