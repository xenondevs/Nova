package xyz.xenondevs.nova.world.item.behavior

import net.minecraft.core.Direction
import net.minecraft.world.level.block.Block
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.block.BlockFace
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockInteract
import xyz.xenondevs.nova.util.center
import xyz.xenondevs.nova.util.nmsDirection
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.InteractionResult
import xyz.xenondevs.nova.world.item.ItemAction
import xyz.xenondevs.nova.world.pos

private val TILLABLES: Map<Material, Triple<(Context<BlockInteract>) -> Boolean, Material, List<Material>>> = mapOf(
    Material.GRASS_BLOCK to Triple(::onlyIfAirAbove, Material.FARMLAND, emptyList()),
    Material.DIRT_PATH to Triple(::onlyIfAirAbove, Material.FARMLAND, emptyList()),
    Material.DIRT to Triple(::onlyIfAirAbove, Material.FARMLAND, emptyList()),
    Material.COARSE_DIRT to Triple(::onlyIfAirAbove, Material.FARMLAND, emptyList()),
    Material.ROOTED_DIRT to Triple({ true }, Material.DIRT, listOf(Material.HANGING_ROOTS))
)

private fun onlyIfAirAbove(ctx: Context<BlockInteract>): Boolean {
    return ctx[BlockInteract.CLICKED_BLOCK_FACE] != BlockFace.DOWN
        && ctx[BlockInteract.BLOCK_POS].add(0, 1, 0).block.type.isAir
}

/**
 * Allows items to till the ground.
 */
object Tilling : ItemBehavior {
    
    override fun useOnBlock(itemStack: ItemStack, block: org.bukkit.block.Block, ctx: Context<BlockInteract>): InteractionResult {
        val (check, newType, drops) = TILLABLES[block.type] ?: return InteractionResult.Pass
        if (!check.invoke(ctx))
            return InteractionResult.Pass
        
        val sourceEntity = ctx[BlockInteract.SOURCE_ENTITY]
        
        // play sound
        block.world.playSound(block.center, Sound.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1f, 1f)
        
        // update block
        block.type = newType
        block.world.sendGameEvent(sourceEntity, org.bukkit.GameEvent.BLOCK_CHANGE, block.location.toVector())
        
        // drop items
        val dropDirection = ctx[BlockInteract.CLICKED_BLOCK_FACE]?.nmsDirection ?: Direction.NORTH
        for (drop in drops) {
            Block.popResourceFromFace(block.world.serverLevel, block.pos.nmsPos, dropDirection, ItemStack.of(drop).unwrap())
        }
        
        return InteractionResult.Success(swing = true, action = ItemAction.Damage())
    }
    
}