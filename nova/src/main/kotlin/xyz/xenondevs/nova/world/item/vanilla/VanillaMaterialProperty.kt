package xyz.xenondevs.nova.world.item.vanilla

enum class VanillaMaterialProperty {
    
    /**
     * The item can't break any blocks in creative.
     */
    CREATIVE_NON_BLOCK_BREAKING,
    
    // this is a vanilla material property because we don't want to make non-dyeable items (shulker shell) dyeable server-side,
    // so instead we can just use an item type that is already dyeable
    // (the client checks whether items are in the dyeable tag for armor rendering)
    /**
     * The item is in the DYEABLE tag.
     */
    DYEABLE,
    
    /**
     * The item can show bundle contents.
     */
    BUNDLE
    
}