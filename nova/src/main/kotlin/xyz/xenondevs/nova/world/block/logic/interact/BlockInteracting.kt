package xyz.xenondevs.nova.world.block.logic.interact

import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.inventory.InventoryCreativeEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.DefaultContextIntentions
import xyz.xenondevs.nova.context.intention.DefaultContextIntentions.BlockBreak
import xyz.xenondevs.nova.context.param.DefaultContextParamTypes
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.util.BlockUtils
import xyz.xenondevs.nova.util.nmsBlock
import xyz.xenondevs.nova.util.nmsState
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.world.player.WrappedPlayerInteractEvent
import xyz.xenondevs.nova.world.pos

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dependsOn = [WorldDataManager::class]
)
internal object BlockInteracting : Listener {
    
    @InitFun
    private fun init() {
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
    private fun handleInventoryCreative(event: InventoryCreativeEvent) {
        if (event.slotType != InventoryType.SlotType.QUICKBAR)
            return
        
        val player = event.whoClicked as Player
        val reach = player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE)?.value ?: 8.0
        val rayTraceResult = player.rayTraceBlocks(reach)
            ?: return
        val targetBlock = rayTraceResult.hitBlock
            ?: return
        val targetPos = targetBlock.pos
        
        val vanillaCloneStack = targetBlock.type.nmsBlock.getCloneItemStack(
            targetBlock.world.serverLevel,
            targetBlock.pos.nmsPos,
            targetBlock.nmsState
        ).asBukkitMirror()
        
        if (vanillaCloneStack != event.cursor)
            return
        
        val novaBlockState = WorldDataManager.getBlockState(targetPos)
            ?: return
        
        val ctx = Context.intention(DefaultContextIntentions.BlockInteract)
            .param(DefaultContextParamTypes.BLOCK_POS, targetPos)
            .param(DefaultContextParamTypes.BLOCK_STATE_NOVA, novaBlockState)
            .param(DefaultContextParamTypes.SOURCE_ENTITY, player)
            .param(DefaultContextParamTypes.CLICKED_BLOCK_FACE, rayTraceResult.hitBlockFace)
            .param(DefaultContextParamTypes.INTERACTION_HAND, EquipmentSlot.HAND)
            .param(DefaultContextParamTypes.INTERACTION_ITEM_STACK, event.cursor)
            .build()
        
        val novaCloneStack = novaBlockState.block.pickBlockCreative(targetPos, novaBlockState, ctx) ?: ItemStack.empty()
        event.cursor = novaCloneStack
        player.updateInventory()
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
    private fun handleEntityExplosion(event: EntityExplodeEvent) = handleExplosion(event.blockList())
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleBlockExplosion(event: BlockExplodeEvent) = handleExplosion(event.blockList())
    
    private fun handleExplosion(blockList: MutableList<Block>) {
        val novaBlocks = blockList.filter { WorldDataManager.getBlockState(it.pos) != null }
        blockList.removeAll(novaBlocks)
        novaBlocks.forEach {
            val ctx = Context.intention(BlockBreak)
                .param(DefaultContextParamTypes.BLOCK_POS, it.pos)
                .param(DefaultContextParamTypes.BLOCK_BREAK_EFFECTS, false)
                .build()
            BlockUtils.breakBlockNaturally(ctx)
        }
    }
    
}