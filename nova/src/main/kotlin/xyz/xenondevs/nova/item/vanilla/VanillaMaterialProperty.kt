package xyz.xenondevs.nova.item.vanilla

enum class VanillaMaterialProperty {
    
    /**
     * The item has a durability bar.
     */
    DAMAGEABLE,
    
    /**
     * The item has a set damage and cooldown.
     */
    DAMAGING_NORMAL,
    
    /**
     * The item has a set damage and cooldown and can also do sweeping attacks.
     */
    DAMAGING_SWEEPING, // TODO: check if this even handled clientside
    
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