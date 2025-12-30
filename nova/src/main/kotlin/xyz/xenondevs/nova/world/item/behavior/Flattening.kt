package xyz.xenondevs.nova.world.item.behavior

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockInteract
import xyz.xenondevs.nova.util.above
import xyz.xenondevs.nova.util.playSoundNearby
import xyz.xenondevs.nova.world.InteractionResult
import xyz.xenondevs.nova.world.item.ItemAction

private val FLATTENABLES: Set<Material> = hashSetOf(
    Material.GRASS_BLOCK,
    Material.DIRT,
    Material.PODZOL,
    Material.COARSE_DIRT,
    Material.MYCELIUM,
    Material.ROOTED_DIRT
)

/**
 * Allows items to flatten the ground.
 */
object Flattening : ItemBehavior {
    
    override fun useOnBlock(itemStack: ItemStack, block: Block, ctx: Context<BlockInteract>): InteractionResult {
        if (block.type !in FLATTENABLES || !block.above.type.isAir)
            return InteractionResult.Pass
        
        block.type = Material.DIRT_PATH
        block.location.playSoundNearby(Sound.ITEM_SHOVEL_FLATTEN, SoundCategory.BLOCKS, 1f, 1f)
        
        return InteractionResult.Success(swing = true, action = ItemAction.Damage())
    }
    
}