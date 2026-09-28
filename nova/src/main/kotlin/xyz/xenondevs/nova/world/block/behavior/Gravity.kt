package xyz.xenondevs.nova.world.block.behavior

import net.minecraft.world.entity.item.FallingBlockEntity
import org.bukkit.GameEvent
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.entity.FallingBlock
import org.bukkit.util.Vector
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockPlace
import xyz.xenondevs.nova.util.BlockUtils
import xyz.xenondevs.nova.util.below
import xyz.xenondevs.nova.util.nmsBlockState
import xyz.xenondevs.nova.util.nmsPos
import xyz.xenondevs.nova.util.scheduleTick
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.world.block.NovaBlockState

/**
 * Makes a block fall when there is no other block below it, then land according to [landingMode].
 * Should not be used for tile-entity blocks.
 */
class Gravity(
    val landingMode: LandingMode = LandingMode.PLACE_OR_DROP
) : BlockBehavior {
    
    override fun handlePlace(block: Block, state: NovaBlockState, ctx: Context<BlockPlace>) {
        block.scheduleTick(2)
    }
    
    override fun updateShape(block: Block, state: NovaBlockState, neighbor: Block, neighborState: BlockData): NovaBlockState {
        block.scheduleTick(2)
        return state
    }
    
    override fun handleScheduledTick(block: Block, state: NovaBlockState) {
        if (block.y < block.world.minHeight)
            return
        
        val below = block.below
        if (!below.isEmpty && !below.isLiquid && !below.blockData.isReplaceable)
            return
        
        val fallingBlock = FallingBlockEntity.fall(block.world.serverLevel, block.nmsPos, state.nmsBlockState)
        when (landingMode) {
            LandingMode.PLACE_OR_DROP -> Unit
            LandingMode.PLACE_OR_DISAPPEAR -> fallingBlock.dropItem = false
            LandingMode.DESTROY -> {
                fallingBlock.dropItem = false
                fallingBlock.disableDrop()
            }
        }
    }
    
    override fun handleFallingBlockDestroy(block: Block, state: NovaBlockState, entity: FallingBlock) {
        if (landingMode != LandingMode.DESTROY)
            return
        
        BlockUtils.playBreakEffects(state, block, null)
        
        val bounds = entity.boundingBox
        block.world.sendGameEvent(
            entity,
            GameEvent.BLOCK_DESTROY,
            Vector(bounds.centerX, bounds.centerY, bounds.centerZ)
        )
    }
    
    /**
     * Controls what happens when a falling block reaches the ground.
     */
    enum class LandingMode {
        
        /**
         * Attempts to place the block and drops its item if placement fails.
         */
        PLACE_OR_DROP,
        
        /**
         * Attempts to place the block and silently disappears if placement fails.
         */
        PLACE_OR_DISAPPEAR,
        
        /**
         * Destroys the block with break effects without attempting placement or dropping an item.
         */
        DESTROY
        
    }
    
}
