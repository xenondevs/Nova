package xyz.xenondevs.nova.world.item.behavior

import kotlinx.coroutines.runBlocking
import net.minecraft.core.component.DataComponents
import net.minecraft.world.level.block.state.pattern.BlockInWorld
import org.bukkit.GameMode
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockInteract
import xyz.xenondevs.nova.context.intention.BlockPlace
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.util.BlockUtils
import xyz.xenondevs.nova.util.bukkitBlockData
import xyz.xenondevs.nova.util.isInsideWorldRestrictions
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.InteractionResult
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.state.model.BackingStateBlockModelProvider
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityBlockModelProvider
import xyz.xenondevs.nova.world.block.state.model.ModelLessBlockModelProvider
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.world.item.ItemAction

internal class BlockItemBehavior(blockType: Provider<NovaBlock>) : ItemBehavior {
    
    private val novaBlock by blockType
    
    override fun useOnBlock(itemStack: ItemStack, block: Block, ctx: Context<BlockInteract>): InteractionResult {
        val player = ctx[BlockInteract.SOURCE_PLAYER] 
            ?: return InteractionResult.Fail
        val handItem = ctx[BlockInteract.HELD_ITEM_STACK]
        var pos = ctx[BlockInteract.BLOCK_POS]
        val clickedFace = ctx[BlockInteract.CLICKED_BLOCK_FACE] ?: BlockFace.NORTH
        if (!Tag.REPLACEABLE.isTagged(pos.block.type) || WorldDataManager.getBlockState(pos) != null)
            pos = pos.advance(clickedFace)
        
        val ctxBuilder = Context.intention(BlockPlace)
            .param(BlockPlace.BLOCK_POS, pos)
            .param(BlockPlace.BLOCK_ITEM_STACK, handItem)
            .param(BlockPlace.SOURCE_ENTITY, player)
            .param(BlockPlace.CLICKED_BLOCK_FACE, ctx[BlockInteract.CLICKED_BLOCK_FACE])
        
        val newState = novaBlock.chooseBlockState(ctxBuilder.build())
        ctxBuilder.param(BlockPlace.BLOCK_STATE_NOVA, newState)
        
        val ctx = ctxBuilder.build()
        
        val vanillaState = when (val modelProvider = newState.modelProvider) {
            is BackingStateBlockModelProvider -> modelProvider.info.vanillaBlockState.bukkitBlockData
            is DisplayEntityBlockModelProvider -> modelProvider.info.hitboxType.bukkitBlockData
            is ModelLessBlockModelProvider -> modelProvider.info.bukkitBlockData
        }
        
        if (pos.location.isInsideWorldRestrictions()
            && BlockUtils.isUnobstructed(pos, player, vanillaState)
            && ProtectionManager.canPlace(player, handItem, pos)
            && canPlace(player, handItem, pos, pos.advance(clickedFace.oppositeFace))
            && runBlocking { novaBlock.canPlace(pos, newState, ctx) } // assume blocking is ok because player is online
        ) {
            BlockUtils.placeNovaBlock(pos, newState, ctx)
            return InteractionResult.Success(swing = true, action = ItemAction.Consume())
        }
        
        return InteractionResult.Fail
    }
    
    private fun canPlace(player: Player, item: ItemStack, block: BlockPos, placedOn: BlockPos): Boolean {
        if (
            player.gameMode == GameMode.SPECTATOR
            || !block.location.isInsideWorldRestrictions()
            || !Tag.REPLACEABLE.isTagged(block.block.type)
            || WorldDataManager.getBlockState(block) != null
        ) return false
        
        if (player.gameMode == GameMode.ADVENTURE) {
            val canPlaceOn = item.unwrap().get(DataComponents.CAN_PLACE_ON)
            val blockInWorld = BlockInWorld(placedOn.world.serverLevel, placedOn.nmsPos, false)
            return canPlaceOn?.test(blockInWorld) ?: false
        }
        
        return true
    }
    
}