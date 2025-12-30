package xyz.xenondevs.nova.world.block.logic.place

import org.bukkit.block.data.type.Dispenser
import org.bukkit.entity.FallingBlock
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDispenseEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketFillEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.util.dropItem
import xyz.xenondevs.nova.util.item.isBucket
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.world.pos

/**
 * Handles in-game block placing by players.
 */
@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dependsOn = [WorldDataManager::class]
)
internal object BlockPlacing : Listener {
    
    @InitFun
    private fun init() {
        registerEvents()
    }
    
    // handleBlockPlace, handleFluidPlace, handleFluidRemove:
    // Prevent players from placing blocks where there are actually already blocks form Nova.
    // This can happen when a replaceable hitbox material, such as structure void, is used.
    // However, we want to permit this for built-in custom blocks, as those might have their
    // WorldDataManager entry set before this event is called (block migration patch)
    
    // requires earlier block place event because BlockMigrator has already removed WorldDataManager entry already otherwise
    fun handleBlockPlace(pos: BlockPos): Boolean {
        val blockState = WorldDataManager.getBlockState(pos)
        return blockState == null || blockState.block.id.namespace() == "nova"
    }
    
    @EventHandler(ignoreCancelled = true)
    private fun handleFluidPlace(event: PlayerBucketEmptyEvent) {
        val blockState = WorldDataManager.getBlockState(event.block.pos)
        event.isCancelled = blockState != null && blockState.block.id.namespace() != "nova"
    }
    
    @EventHandler(ignoreCancelled = true)
    private fun handleFluidRemove(event: PlayerBucketFillEvent) {
        val blockState = WorldDataManager.getBlockState(event.block.pos)
        event.isCancelled = blockState != null && blockState.block.id.namespace() != "nova"
    }
    
    @EventHandler(ignoreCancelled = true)
    private fun handleFallingBlockLand(event: EntityChangeBlockEvent) {
        val entity = event.entity
        if (entity is FallingBlock) {
            val pos = event.block.pos
            val blockState = WorldDataManager.getBlockState(pos)
            if (blockState != null) {
                event.isCancelled = true
                event.block.location.dropItem(ItemStack(entity.blockData.material))
            }
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    private fun handleBlockDispense(event: BlockDispenseEvent) {
        if (event.item.type.isBucket()) {
            val targetPos = event.block.pos.advance((event.block.blockData as Dispenser).facing)
            if (WorldDataManager.getBlockStateOrNullIfUnloaded(targetPos) != null) {
                event.isCancelled = true
            }
        }
    }
    
}