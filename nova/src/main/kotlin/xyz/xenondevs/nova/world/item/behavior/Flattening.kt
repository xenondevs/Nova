package xyz.xenondevs.nova.world.item.behavior

import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.block.Block
import org.bukkit.block.BlockType
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockInteract
import xyz.xenondevs.nova.registry.entries.BlockTypeEntries
import xyz.xenondevs.nova.registry.registryEntrySetOf
import xyz.xenondevs.nova.util.above
import xyz.xenondevs.nova.util.playSoundNearby
import xyz.xenondevs.nova.world.InteractionResult
import xyz.xenondevs.nova.world.block.blockType
import xyz.xenondevs.nova.world.item.ItemAction

private val FLATTENABLES = registryEntrySetOf(
    BlockTypeEntries.GRASS_BLOCK,
    BlockTypeEntries.DIRT,
    BlockTypeEntries.PODZOL,
    BlockTypeEntries.COARSE_DIRT,
    BlockTypeEntries.MYCELIUM,
    BlockTypeEntries.ROOTED_DIRT
)

/**
 * Allows items to flatten the ground.
 */
object Flattening : ItemBehavior {
    
    override fun useOnBlock(itemStack: ItemStack, block: Block, ctx: Context<BlockInteract>): InteractionResult {
        if (block.blockType !in FLATTENABLES || !block.above.blockType.isAir)
            return InteractionResult.Pass
        
        block.blockType = BlockType.DIRT_PATH
        block.location.playSoundNearby(Sound.ITEM_SHOVEL_FLATTEN, SoundCategory.BLOCKS, 1f, 1f)
        
        return InteractionResult.Success(swing = true, action = ItemAction.Damage())
    }
    
}
