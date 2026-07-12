package xyz.xenondevs.nova.world.item.behavior

import kotlinx.coroutines.runBlocking
import net.minecraft.core.component.DataComponents
import net.minecraft.world.level.block.state.pattern.BlockInWorld
import org.bukkit.GameMode
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockInteract
import xyz.xenondevs.nova.context.intention.BlockPlace
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.registry.entries.BlockTypeTags
import xyz.xenondevs.nova.util.BlockUtils
import xyz.xenondevs.nova.util.isInsideWorldRestrictions
import xyz.xenondevs.nova.util.nmsPos
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.InteractionResult
import xyz.xenondevs.nova.world.block.NovaBlockState
import xyz.xenondevs.nova.world.block.blockType
import xyz.xenondevs.nova.world.block.clientsideBlockState
import xyz.xenondevs.nova.world.block.novaBlock
import xyz.xenondevs.nova.world.item.ItemAction

internal class BlockItemBehavior(blockType: Provider<BlockType>) : ItemBehavior {
    
    private val blockType by blockType
    
    override fun useOnBlock(itemStack: ItemStack, block: Block, ctx: Context<BlockInteract>): InteractionResult {
        val player = ctx[BlockInteract.SOURCE_PLAYER]
            ?: return InteractionResult.Fail
        val handItem = ctx[BlockInteract.HELD_ITEM_STACK]
        var block = ctx[BlockInteract.BLOCK]
        val clickedFace = ctx[BlockInteract.CLICKED_BLOCK_FACE] ?: BlockFace.NORTH
        if (block.blockType !in BlockTypeTags.REPLACEABLE)
            block = block.getRelative(clickedFace)
        
        val ctx = Context.intention(BlockPlace)
            .param(BlockPlace.BLOCK, block)
            .param(BlockPlace.BLOCK_TYPE, blockType)
            .param(BlockPlace.PREVIOUS_BLOCK_STATE, block.blockData)
            .param(BlockPlace.BLOCK_ITEM_STACK, handItem)
            .param(BlockPlace.SOURCE_ENTITY, player)
            .param(BlockPlace.CLICKED_BLOCK_FACE, ctx[BlockInteract.CLICKED_BLOCK_FACE])
            .param(BlockPlace.HELD_HAND, ctx[BlockInteract.HELD_HAND])
            .param(BlockPlace.HELD_ITEM_STACK, handItem)
            .build()
        
        val newState = ctx[BlockPlace.BLOCK_STATE]
        require(newState is NovaBlockState)
        
        val vanillaState = newState.clientsideBlockState
        
        if (block.location.isInsideWorldRestrictions()
            && BlockUtils.isUnobstructed(block, player, vanillaState)
            && ProtectionManager.canPlace(player, handItem, block)
            && canPlace(player, handItem, block, block.getRelative(clickedFace.oppositeFace, 1))
            && runBlocking { blockType.novaBlock?.canPlace(block, newState, ctx) != false } // assume blocking is ok because player is online
        ) {
            BlockUtils.placeBlock(ctx)
            return InteractionResult.Success(swing = true, action = ItemAction.Consume())
        }
        
        return InteractionResult.Fail
    }
    
    private fun canPlace(player: Player, item: ItemStack, block: Block, placedOn: Block): Boolean {
        if (
            player.gameMode == GameMode.SPECTATOR
            || !block.location.isInsideWorldRestrictions()
            || block.blockType !in BlockTypeTags.REPLACEABLE
        ) return false
        
        if (player.gameMode == GameMode.ADVENTURE) {
            val canPlaceOn = item.unwrap().get(DataComponents.CAN_PLACE_ON)
            val blockInWorld = BlockInWorld(placedOn.world.serverLevel, placedOn.nmsPos, false)
            return canPlaceOn?.test(blockInWorld) ?: false
        }
        
        return true
    }
    
}
