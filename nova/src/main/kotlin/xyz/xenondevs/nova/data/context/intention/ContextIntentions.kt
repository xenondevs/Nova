package xyz.xenondevs.nova.data.context.intention

import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes.BLOCK_BREAK_EFFECTS
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes.BLOCK_DROPS
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes.BLOCK_ITEM_STACK
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes.BLOCK_PLACE_EFFECTS
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes.BLOCK_POS
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes.BLOCK_STATE_NOVA
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes.BLOCK_STORAGE_DROPS
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes.BLOCK_TYPE
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes.BLOCK_TYPE_NOVA
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes.BLOCK_TYPE_VANILLA
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes.BLOCK_WORLD
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes.BYPASS_TILE_ENTITY_LIMITS
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes.CLICKED_BLOCK_FACE
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes.INTERACTION_HAND
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes.INTERACTION_ITEM_STACK
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes.RESPONSIBLE_PLAYER
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes.SOURCE_DIRECTION
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes.SOURCE_ENTITY
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes.SOURCE_LOCATION
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes.SOURCE_PLAYER
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes.SOURCE_TILE_ENTITY
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes.SOURCE_UUID
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes.SOURCE_WORLD
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes.TILE_ENTITY_DATA_NOVA
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes.TOOL_ITEM_STACK

/**
 * Contains all built-in [context intentions][ContextIntention].
 */
object ContextIntentions {
    
    /**
     * The intention to place a block.
     */
    data object BlockPlace : ContextIntention(
        required = listOf(BLOCK_POS, BLOCK_WORLD, BLOCK_TYPE),
        optional = listOf(
            BLOCK_ITEM_STACK,
            BLOCK_TYPE_NOVA, BLOCK_STATE_NOVA, BLOCK_TYPE_VANILLA, CLICKED_BLOCK_FACE,
            TILE_ENTITY_DATA_NOVA,
            SOURCE_UUID, SOURCE_ENTITY, SOURCE_PLAYER, SOURCE_TILE_ENTITY, SOURCE_LOCATION, SOURCE_WORLD, SOURCE_DIRECTION,
            RESPONSIBLE_PLAYER,
            BLOCK_PLACE_EFFECTS, BYPASS_TILE_ENTITY_LIMITS
        )
    )
    
    /**
     * The intention to break a block.
     */
    data object BlockBreak : ContextIntention(
        required = listOf(BLOCK_POS, BLOCK_WORLD),
        optional = listOf(
            TOOL_ITEM_STACK,
            BLOCK_TYPE, BLOCK_TYPE_VANILLA, BLOCK_TYPE_NOVA, CLICKED_BLOCK_FACE, BLOCK_DROPS, BLOCK_STORAGE_DROPS,
            SOURCE_UUID, SOURCE_ENTITY, SOURCE_PLAYER, SOURCE_TILE_ENTITY, SOURCE_LOCATION, SOURCE_WORLD, SOURCE_DIRECTION,
            RESPONSIBLE_PLAYER,
            BLOCK_BREAK_EFFECTS
        )
    )
    
    /**
     * The intention to interact with a block.
     */
    data object BlockInteract : ContextIntention(
        required = listOf(BLOCK_POS, BLOCK_WORLD),
        optional = listOf(
            INTERACTION_HAND, INTERACTION_ITEM_STACK,
            BLOCK_TYPE, BLOCK_TYPE_VANILLA, BLOCK_TYPE_NOVA, CLICKED_BLOCK_FACE,
            SOURCE_UUID, SOURCE_ENTITY, SOURCE_PLAYER, SOURCE_TILE_ENTITY, SOURCE_LOCATION, SOURCE_WORLD, SOURCE_DIRECTION,
            RESPONSIBLE_PLAYER
        )
    )
    
}