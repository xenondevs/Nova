package xyz.xenondevs.nova.world.block.logic.interact

import org.bukkit.block.Block
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.DefaultContextIntentions
import xyz.xenondevs.nova.context.intention.DefaultContextIntentions.BlockBreak
import xyz.xenondevs.nova.context.param.DefaultContextParamTypes
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.network.event.PacketListener
import xyz.xenondevs.nova.network.event.registerPacketListener
import xyz.xenondevs.nova.util.BlockUtils
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.world.player.WrappedPlayerInteractEvent
import xyz.xenondevs.nova.world.pos

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dependsOn = [WorldDataManager::class]
)
internal object BlockInteracting : Listener, PacketListener {
    
    @InitFun
    private fun init() {
        registerEvents()
        registerPacketListener()
    }
    
    @EventHandler(priority = EventPriority.LOW)
    fun handleInteract(wrappedEvent: WrappedPlayerInteractEvent) {
        if (wrappedEvent.actionPerformed)
            return
        
        val event = wrappedEvent.event
        val player = event.player
        if (event.action == Action.RIGHT_CLICK_BLOCK) {
            val pos = event.clickedBlock!!.pos
            
            val blockState = WorldDataManager.getBlockState(pos)
            if (blockState != null && ProtectionManager.canUseBlock(player, event.item, pos)) {
                val block = blockState.block
                
                val ctx = Context.intention(DefaultContextIntentions.BlockInteract)
                    .param(DefaultContextParamTypes.BLOCK_POS, pos)
                    .param(DefaultContextParamTypes.BLOCK_TYPE_NOVA, block)
                    .param(DefaultContextParamTypes.SOURCE_ENTITY, player)
                    .param(DefaultContextParamTypes.CLICKED_BLOCK_FACE, event.blockFace)
                    .param(DefaultContextParamTypes.INTERACTION_HAND, event.hand)
                    .param(DefaultContextParamTypes.INTERACTION_ITEM_STACK, event.item)
                    .build()
                
                val actionPerformed = block.handleInteract(pos, blockState, ctx)
                event.isCancelled = actionPerformed
                wrappedEvent.actionPerformed = actionPerformed
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handlePistonExtend(event: BlockPistonExtendEvent) {
        if (event.blocks.any { WorldDataManager.getBlockState(it.pos) != null }) event.isCancelled = true
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handlePistonRetract(event: BlockPistonRetractEvent) {
        if (event.blocks.any { WorldDataManager.getBlockState(it.pos) != null }) event.isCancelled = true
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun handleEntityChangeBlock(event: EntityChangeBlockEvent) {
        val type = event.entityType
        if ((type == EntityType.SILVERFISH || type == EntityType.ENDERMAN) && WorldDataManager.getBlockState(event.block.pos) != null)
            event.isCancelled = true
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleEntityExplosion(event: EntityExplodeEvent) {
        if (event.entityType == EntityType.WIND_CHARGE)
            return // wind charges don't destroy blocks
        handleExplosion(event.blockList())
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleBlockExplosion(event: BlockExplodeEvent) = handleExplosion(event.blockList())
    
    private fun handleExplosion(blockList: MutableList<Block>) {
        val novaBlocks = blockList.filter { WorldDataManager.getBlockState(it.pos) != null }
        blockList.removeAll(novaBlocks)
        novaBlocks.forEach {
            val ctx = Context.intention(BlockBreak)
                .param(DefaultContextParamTypes.BLOCK_POS, it.pos)
                .param(DefaultContextParamTypes.BLOCK_BREAK_EFFECTS, false)
                .param(DefaultContextParamTypes.BLOCK_DROPS, true)
                .build()
            BlockUtils.breakBlockNaturally(ctx)
        }
    }
    
}