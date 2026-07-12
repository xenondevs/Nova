package xyz.xenondevs.nova.world.item.behavior

import net.minecraft.core.Direction
import net.minecraft.world.level.block.Block
import org.bukkit.GameEvent
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockInteract
import xyz.xenondevs.nova.util.center
import xyz.xenondevs.nova.util.nmsDirection
import xyz.xenondevs.nova.util.nmsPos
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.InteractionResult
import xyz.xenondevs.nova.world.block.blockType
import xyz.xenondevs.nova.world.item.ItemAction

private val TILLABLES: Map<BlockType, Triple<(Context<BlockInteract>) -> Boolean, BlockType, List<ItemType>>> = mapOf(
    BlockType.GRASS_BLOCK to Triple(::onlyIfAirAbove, BlockType.FARMLAND, emptyList()),
    BlockType.DIRT_PATH to Triple(::onlyIfAirAbove, BlockType.FARMLAND, emptyList()),
    BlockType.DIRT to Triple(::onlyIfAirAbove, BlockType.FARMLAND, emptyList()),
    BlockType.COARSE_DIRT to Triple(::onlyIfAirAbove, BlockType.FARMLAND, emptyList()),
    BlockType.ROOTED_DIRT to Triple({ true }, BlockType.DIRT, listOf(ItemType.HANGING_ROOTS))
)

private fun onlyIfAirAbove(ctx: Context<BlockInteract>): Boolean {
    return ctx[BlockInteract.CLICKED_BLOCK_FACE] != BlockFace.DOWN
        && ctx[BlockInteract.BLOCK].getRelative(BlockFace.UP).blockType.isAir
}

/**
 * Allows items to till the ground.
 */
object Tilling : ItemBehavior {
    
    override fun useOnBlock(itemStack: ItemStack, block: org.bukkit.block.Block, ctx: Context<BlockInteract>): InteractionResult {
        val [check, newType, drops] = TILLABLES[block.blockType] ?: return InteractionResult.Pass
        if (!check.invoke(ctx))
            return InteractionResult.Pass
        
        val sourceEntity = ctx[BlockInteract.SOURCE_ENTITY]
        
        // play sound
        block.world.playSound(block.center, Sound.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1f, 1f)
        
        // update block
        block.blockType = newType
        block.world.sendGameEvent(sourceEntity, GameEvent.BLOCK_CHANGE, block.location.toVector())
        
        // drop items
        val dropDirection = ctx[BlockInteract.CLICKED_BLOCK_FACE]?.nmsDirection ?: Direction.NORTH
        for (drop in drops) {
            Block.popResourceFromFace(block.world.serverLevel, block.nmsPos, dropDirection, drop.createItemStack().unwrap())
        }
        
        return InteractionResult.Success(swing = true, action = ItemAction.Damage())
    }
    
}
