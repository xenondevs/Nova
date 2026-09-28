package xyz.xenondevs.nova.context.intention

import xyz.xenondevs.nova.context.Context

internal object ImplicitIntentions {
    
    val BLOCK_PLACE: ScopedValue<Context<BlockPlace>> = ScopedValue.newInstance()
    val BLOCK_BREAK: ScopedValue<Context<BlockBreak>> = ScopedValue.newInstance()
    val BLOCK_INTERACT: ScopedValue<Context<BlockInteract>> = ScopedValue.newInstance()
    val ENTITY_INTERACT: ScopedValue<Context<EntityInteract>> = ScopedValue.newInstance()
    val ITEM_USE: ScopedValue<Context<ItemUse>> = ScopedValue.newInstance()
    
}