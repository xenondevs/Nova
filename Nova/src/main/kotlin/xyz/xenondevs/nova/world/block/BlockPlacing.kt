package xyz.xenondevs.nova.world.block

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.material.BlockNovaMaterial
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.util.concurrent.CombinedBooleanFuture
import xyz.xenondevs.nova.util.concurrent.runIfTrue
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
import xyz.xenondevs.nova.world.pos

internal object BlockPlacing : Listener {
    
    fun init() {
        Bukkit.getPluginManager().registerEvents(this, NOVA)
    }
    
    @EventHandler
    private fun handleBlockPlace(event: BlockPlaceEvent) {
        // Prevent players from placing blocks where there are actually already blocks form Nova
        // This can happen when the hitbox material is replaceable, like as structure void
        if (WorldDataManager.getBlockState(event.block.pos) != null)
            event.isCancelled = true
    }
    
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private fun handleInteract(event: PlayerInteractEvent) {
        val action = event.action
        val player = event.player
        if (action == Action.RIGHT_CLICK_BLOCK) {
            val handItem = event.item
            val block = event.clickedBlock!!
            
            if (!block.type.isActuallyInteractable() || player.isSneaking) {
                val material = handItem?.novaMaterial
                if (material is BlockNovaMaterial) {
                    placeNovaBlock(event, material)
                } else if (
                    WorldDataManager.getBlockState(block.pos) != null // the block placed against is from Nova
                    && block.type.isReplaceable() // and will be replaced without special behavior
                    && material == null
                    && handItem?.type?.isBlock == true // a vanilla block material is used 
                ) placeVanillaBlock(event)
            }
        }
    }
    
    private fun placeNovaBlock(event: PlayerInteractEvent, material: BlockNovaMaterial) {
        event.isCancelled = true
        
        val player = event.player
        val handItem = event.item!!
        val playerLocation = player.location
        val placeLoc = event.clickedBlock!!.location.advance(event.blockFace)
        
        val placeFuture = if (material.placeCheck != null) {
            CombinedBooleanFuture(
                ProtectionManager.canPlace(player, handItem, placeLoc),
                material.placeCheck.invoke(player, handItem, placeLoc.apply { yaw = playerLocation.facing.oppositeFace.yaw })
            )
        } else ProtectionManager.canPlace(player, handItem, placeLoc)
        
        placeFuture.runIfTrue {
            if (placeLoc.block.type.isReplaceable() && WorldDataManager.getBlockState(placeLoc.pos) == null) {
                // TODO: Block Limits
                val ctx = BlockPlaceContext(
                    placeLoc.pos, handItem,
                    player, player.location, player.uniqueId,
                    event.clickedBlock!!.pos, event.blockFace
                )
                
                BlockManager.placeBlock(material, ctx)
                
                if (player.gameMode == GameMode.SURVIVAL) handItem.amount--
                player.swingMainHand()
            }
        }
    }
    
    private fun placeVanillaBlock(event: PlayerInteractEvent) {
        event.isCancelled = true
        
        val player = event.player
        val handItem = event.item!!
        val block = event.clickedBlock!!
        
        val replaceLocation = block.location.advance(event.blockFace)
        val replaceBlock = replaceLocation.block
        
        // check if the player is allowed to place a block there
        ProtectionManager.canPlace(player, handItem, replaceLocation).runIfTrue {
            // check that there isn't already a block there (which is not replaceable)
            if (replaceBlock.type.isReplaceable() && WorldDataManager.getBlockState(replaceBlock.pos) == null) {
                // mimic the way the actual BlockPlaceEvent works by first setting the block type,
                // then calling the event and reverting if it is cancelled
                val previousType = replaceBlock.type
                val previousData = replaceBlock.blockData
                
                replaceBlock.type = handItem.type
                
                val placeEvent = BlockPlaceEvent(replaceBlock, replaceBlock.state, block, handItem, player, true, event.hand!!)
                Bukkit.getPluginManager().callEvent(placeEvent)
                
                if (placeEvent.isCancelled) {
                    replaceBlock.type = previousType
                    replaceBlock.blockData = previousData
                } else if (player.gameMode != GameMode.CREATIVE) {
                    player.inventory.setItem(event.hand!!, handItem.apply { amount -= 1 })
                }
            }
        }
    }
    
}