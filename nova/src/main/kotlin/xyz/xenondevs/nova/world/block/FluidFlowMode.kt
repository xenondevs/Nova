package xyz.xenondevs.nova.world.block

import xyz.xenondevs.nova.world.block.behavior.Waterloggable

/**
 * Controls how fluid flows into and out of a block.
 */
enum class FluidFlowMode(
    /**
     * Whether fluid is allowed to flow into the block's position.
     */
    val allowsIncomingFlow: Boolean,
    /**
     * Whether incoming fluid waterlogs the block instead of replacing it.
     */
    val waterlogsIncomingFlow: Boolean,
    /**
     * Whether fluid at the block's position is allowed to flow out.
     */
    val allowsOutgoingFlow: Boolean,
    /**
     * Whether this mode requires the block to have a waterlogged state.
     */
    val requiresWaterloggedState: Boolean
) {
    
    /**
     * Prevents fluid from entering the block and waterlogged fluid from flowing out.
     */
    BLOCK(
        allowsIncomingFlow = false,
        waterlogsIncomingFlow = false,
        allowsOutgoingFlow = false,
        requiresWaterloggedState = false
    ),
    
    /**
     * Replaces the block with incoming fluid.
     */
    BREAK(
        allowsIncomingFlow = true,
        waterlogsIncomingFlow = false,
        allowsOutgoingFlow = true,
        requiresWaterloggedState = false
    ),
    
    /**
     * Allows incoming water to waterlog the block, but prevents the stored water from flowing out.
     * Note that the block will stay waterlogged, even when fluid stops flowing into it.
     *
     * Requires the [Waterloggable] behavior.
     */
    WATERLOG_IN(
        allowsIncomingFlow = true,
        waterlogsIncomingFlow = true,
        allowsOutgoingFlow = false,
        requiresWaterloggedState = true
    ),
    
    /**
     * Prevents flowing water from entering the block, but allows stored water to flow out.
     *
     * Requires the [Waterloggable] behavior.
     */
    WATERLOG_OUT(
        allowsIncomingFlow = false,
        waterlogsIncomingFlow = false,
        allowsOutgoingFlow = true,
        requiresWaterloggedState = true
    ),
    
    /**
     * Allows incoming water to waterlog the block and stored water to flow out.
     * Note that this essentially creates new source blocks.
     *
     * Requires the [Waterloggable] behavior.
     */
    WATERLOG_IN_OUT(
        allowsIncomingFlow = true,
        waterlogsIncomingFlow = true,
        allowsOutgoingFlow = true,
        requiresWaterloggedState = true
    );
    
}
