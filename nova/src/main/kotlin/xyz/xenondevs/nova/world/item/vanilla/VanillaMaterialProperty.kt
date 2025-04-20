package xyz.xenondevs.nova.world.item.vanilla

/**
 * Represents non-data-driven properties of items that are hardcoded to specific item types.
 */
enum class VanillaMaterialProperty {
    
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