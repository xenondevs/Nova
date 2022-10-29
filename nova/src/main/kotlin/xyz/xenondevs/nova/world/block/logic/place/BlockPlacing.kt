package xyz.xenondevs.nova.world.block.logic.place

import net.md_5.bungee.api.ChatColor
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockMultiPlaceEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.material.BlockNovaMaterial
import xyz.xenondevs.nova.player.WrappedPlayerInteractEvent
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.util.concurrent.CombinedBooleanFuture
import xyz.xenondevs.nova.util.concurrent.runIfTrue
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.facing
import xyz.xenondevs.nova.util.isCompletelyDenied
import xyz.xenondevs.nova.util.isInsideWorldRestrictions
import xyz.xenondevs.nova.util.isUnobstructed
import xyz.xenondevs.nova.util.item.isActuallyInteractable
import xyz.xenondevs.nova.util.item.isReplaceable
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.placeVanilla
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.util.swingHand
import xyz.xenondevs.nova.util.yaw
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
import xyz.xenondevs.nova.world.block.limits.TileEntityLimits
import xyz.xenondevs.nova.world.pos
import java.util.concurrent.CompletableFuture

internal object BlockPlacing : Listener {
    
    fun init() {
        registerEvents()
    }
    
    @EventHandler(ignoreCancelled = true)
    private fun handleBlockPlace(event: BlockPlaceEvent) {
        // Prevent players from placing blocks where there are actually already blocks form Nova
        // This can happen when the hitbox material is replaceable, like as structure void
        event.isCancelled = WorldDataManager.getBlockState(event.block.pos) is NovaBlockState
    }
    
    @EventHandler(ignoreCancelled = true)
    private fun handleBlockPlace(event: BlockMultiPlaceEvent) {
        // Prevent players from placing blocks where there are actually already blocks form Nova
        // This can happen when the hitbox material is replaceable, like as structure void
        event.isCancelled = event.replacedBlockStates.any { WorldDataManager.getBlockState(it.location.pos) is NovaBlockState }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    private fun handleInteract(e: WrappedPlayerInteractEvent) {
        val event = e.event
        if (event.isCompletelyDenied()) return
        
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
                    BlockManager.hasBlock(block.pos) // the block placed against is from Nova
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
        
        val clicked = event.clickedBlock!!
        val placeLoc: Location =
            if (clicked.type.isReplaceable() && !BlockManager.hasBlock(clicked.pos))
                clicked.location
            else clicked.location.advance(event.blockFace)
        
        if (!placeLoc.isInsideWorldRestrictions() || !placeLoc.block.isUnobstructed(material.vanillaBlockMaterial, player))
            return
        
        val futures = ArrayList<CompletableFuture<Boolean>>()
        futures += ProtectionManager.canPlace(player, handItem, placeLoc)
        material.multiBlockLoader
            ?.invoke(placeLoc.pos)
            ?.forEach {
                val multiBlockLoc = it.location
                if (!multiBlockLoc.isInsideWorldRestrictions())
                    return
                futures += ProtectionManager.canPlace(player, handItem, multiBlockLoc)
            }
        material.placeCheck
            ?.invoke(player, handItem, placeLoc.apply { yaw = playerLocation.facing.oppositeFace.yaw })
            ?.also(futures::add)
        
        CombinedBooleanFuture(futures).runIfTrue {
            if (!placeLoc.block.type.isReplaceable() || WorldDataManager.getBlockState(placeLoc.pos) != null)
                return@runIfTrue
            
            val ctx = BlockPlaceContext(
                placeLoc.pos, handItem,
                player, player.location, player.uniqueId,
                event.clickedBlock!!.pos, event.blockFace
            )
            
            val result = TileEntityLimits.canPlace(ctx)
            if (result.allowed) {
                BlockManager.placeBlock(material, ctx)
                
                if (player.gameMode == GameMode.SURVIVAL) handItem.amount--
                runTask { player.swingHand(event.hand!!) }
            } else {
                player.spigot().sendMessage(localized(ChatColor.RED, result.message))
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
        if (replaceLocation.isInsideWorldRestrictions()) {
            ProtectionManager.canPlace(player, handItem, replaceLocation).runIfTrue {
                // check that there isn't already a block there (which is not replaceable)
                if (replaceBlock.type.isReplaceable() && WorldDataManager.getBlockState(replaceBlock.pos) == null) {
                    val placed = replaceBlock.placeVanilla(player.serverPlayer, handItem, true)
                    if (placed && player.gameMode != GameMode.CREATIVE) {
                        player.inventory.setItem(event.hand!!, handItem.apply { amount -= 1 })
                    }
                }
            }
        }
    }
    
}