package xyz.xenondevs.nova.data.context.intention

import xyz.xenondevs.nova.data.context.param.ContextParamTypes.BLOCK_BREAK_EFFECTS
import xyz.xenondevs.nova.data.context.param.ContextParamTypes.BLOCK_FACING
import xyz.xenondevs.nova.data.context.param.ContextParamTypes.BLOCK_ITEM_STACK
import xyz.xenondevs.nova.data.context.param.ContextParamTypes.BLOCK_PLACE_EFFECTS
import xyz.xenondevs.nova.data.context.param.ContextParamTypes.BLOCK_POS
import xyz.xenondevs.nova.data.context.param.ContextParamTypes.BLOCK_TYPE
import xyz.xenondevs.nova.data.context.param.ContextParamTypes.BLOCK_TYPE_NOVA
import xyz.xenondevs.nova.data.context.param.ContextParamTypes.BLOCK_TYPE_VANILLA
import xyz.xenondevs.nova.data.context.param.ContextParamTypes.BLOCK_WORLD
import xyz.xenondevs.nova.data.context.param.ContextParamTypes.BYPASS_TILE_ENTITY_LIMITS
import xyz.xenondevs.nova.data.context.param.ContextParamTypes.CLICKED_BLOCK_FACE
import xyz.xenondevs.nova.data.context.param.ContextParamTypes.INTERACTION_HAND
import xyz.xenondevs.nova.data.context.param.ContextParamTypes.SOURCE_DIRECTION
import xyz.xenondevs.nova.data.context.param.ContextParamTypes.SOURCE_ENTITY
import xyz.xenondevs.nova.data.context.param.ContextParamTypes.SOURCE_LOCATION
import xyz.xenondevs.nova.data.context.param.ContextParamTypes.SOURCE_TILE_ENTITY
import xyz.xenondevs.nova.data.context.param.ContextParamTypes.SOURCE_UUID
import xyz.xenondevs.nova.data.context.param.ContextParamTypes.SOURCE_WORLD
import xyz.xenondevs.nova.data.context.param.ContextParamTypes.TOOL_ITEM_STACK

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
            BLOCK_TYPE_NOVA, BLOCK_TYPE_VANILLA,
            BLOCK_ITEM_STACK, BLOCK_FACING,
            SOURCE_ENTITY, SOURCE_TILE_ENTITY, SOURCE_LOCATION, SOURCE_WORLD, SOURCE_DIRECTION, SOURCE_UUID,
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
            CLICKED_BLOCK_FACE,
            SOURCE_ENTITY, SOURCE_TILE_ENTITY, SOURCE_LOCATION, SOURCE_WORLD, SOURCE_DIRECTION, SOURCE_UUID,
            BLOCK_BREAK_EFFECTS
        )
    )
    
    /**
     * The intention to interact with a block.
     */
    data object BlockInteract : ContextIntention(
        required = listOf(BLOCK_POS, BLOCK_WORLD),
        optional = listOf(
            INTERACTION_HAND, TOOL_ITEM_STACK,
            CLICKED_BLOCK_FACE,
            SOURCE_ENTITY, SOURCE_TILE_ENTITY, SOURCE_LOCATION, SOURCE_WORLD, SOURCE_DIRECTION, SOURCE_UUID
        )
    )
    
}