package xyz.xenondevs.nova.context.intention

import xyz.xenondevs.nova.context.AbstractContextIntention

/**
 * Context intention for when an item is used (right-click in air).
 *
 * ## Autofillers
 *
 * Inherits autofillers from [HasHeldItem] and [HasOptionalSource].
 */
object ItemUse :
    AbstractContextIntention<ItemUse>(),
    HasHeldItem<ItemUse>,
    HasOptionalSource<ItemUse> {
    
    init {
        HasHeldItem.applyDefaults(this)
        HasOptionalSource.applyDefaults(this)
    }
    
}