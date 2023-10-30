package xyz.xenondevs.nova.world.block.logic.interact

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.inventory.InventoryCreativeEvent
import xyz.xenondevs.nova.data.context.Context
import xyz.xenondevs.nova.data.context.intention.ContextIntentions
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockBreak
import xyz.xenondevs.nova.data.context.param.ContextParamTypes
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.player.WrappedPlayerInteractEvent
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.pos

internal object BlockInteracting : Listener {
    
    fun init() {
        registerEvents()
    }
    
    @EventHandler(priority = EventPriority.LOW)
    fun handleInteract(wrappedEvent: WrappedPlayerInteractEvent) {
        if (wrappedEvent.actionPerformed)
            return
        
        val event = wrappedEvent.event
        val player = event.player
        if (event.action == Action.RIGHT_CLICK_BLOCK && !player.isSneaking) {
            val pos = event.clickedBlock!!.pos
            
            val blockState = BlockManager.getBlockState(pos)
            if (blockState != null && ProtectionManager.canUseBlock(player, event.item, pos.location).get()) {
                val material = blockState.block
                
                val ctx = Context.intention(ContextIntentions.BlockInteract)
                    .param(ContextParamTypes.BLOCK_POS, pos)
                    .param(ContextParamTypes.BLOCK_TYPE_NOVA, material)
                    .param(ContextParamTypes.SOURCE_ENTITY, player)
                    .param(ContextParamTypes.CLICKED_BLOCK_FACE, event.blockFace)
                    .param(ContextParamTypes.INTERACTION_HAND, event.hand)
                    .param(ContextParamTypes.INTERACTION_ITEM_STACK, event.item)
                    .build()
                
                val actionPerformed = material.logic.handleInteract(blockState, ctx)
                event.isCancelled = actionPerformed
                wrappedEvent.actionPerformed = actionPerformed
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleInventoryCreative(event: InventoryCreativeEvent) {
        val player = event.whoClicked as Player
        val targetBlock = player.getTargetBlockExact(8)
        if (targetBlock != null && targetBlock.type == event.cursor.type) {
            val item = BlockManager.getBlockState(targetBlock.pos)?.block?.item
            if (item != null) event.cursor = item.createItemStack()
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handlePistonExtend(event: BlockPistonExtendEvent) {
        if (event.blocks.any { BlockManager.getBlockState(it.pos) != null }) event.isCancelled = true
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handlePistonRetract(event: BlockPistonRetractEvent) {
        if (event.blocks.any { BlockManager.getBlockState(it.pos) != null }) event.isCancelled = true
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun handleBlockPhysics(event: BlockPhysicsEvent) {
        val pos = event.block.pos
        val state = BlockManager.getBlockState(pos)
        if (state != null && Material.AIR == event.block.type) {
            val ctx = Context.intention(BlockBreak)
                .param(ContextParamTypes.BLOCK_POS, pos)
                .param(ContextParamTypes.BLOCK_BREAK_EFFECTS, false)
                .build()
            BlockManager.breakBlockState(ctx)
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun handleEntityChangeBlock(event: EntityChangeBlockEvent) {
        val type = event.entityType
        if ((type == EntityType.SILVERFISH || type == EntityType.ENDERMAN) && BlockManager.getBlockState(event.block.pos) != null)
            event.isCancelled = true
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleEntityExplosion(event: EntityExplodeEvent) = handleExplosion(event.blockList())
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleBlockExplosion(event: BlockExplodeEvent) = handleExplosion(event.blockList())
    
    private fun handleExplosion(blockList: MutableList<Block>) {
        val novaBlocks = blockList.filter { BlockManager.getBlockState(it.pos) != null }
        blockList.removeAll(novaBlocks)
        novaBlocks.forEach {
            val ctx = Context.intention(BlockBreak)
                .param(ContextParamTypes.BLOCK_POS, it.pos)
                .param(ContextParamTypes.BLOCK_BREAK_EFFECTS, false)
                .build()
            BlockManager.breakBlockState(ctx)
        }
    }
    
}