package xyz.xenondevs.nova.world.block.logic.`break`

import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.*
import net.minecraft.world.InteractionHand
import org.bukkit.GameMode
import org.bukkit.craftbukkit.v1_19_R2.event.CraftEventFactory
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerQuitEvent
import xyz.xenondevs.nmsutils.network.event.PacketHandler
import xyz.xenondevs.nmsutils.network.event.serverbound.ServerboundPlayerActionPacketEvent
import xyz.xenondevs.nmsutils.network.packetHandler
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.registerPacketListener
import xyz.xenondevs.nova.util.removeIf
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.util.send
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.util.serverTick
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.BlockManager
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

private val BREAK_COOLDOWN by configReloadable { DEFAULT_CONFIG.getInt("world.block_breaking.break_cooldown") }

internal object BlockBreaking : Listener {
    
    private val breakCooldowns = ConcurrentHashMap<Player, Int>()
    private val playerBreakers = ConcurrentHashMap<Player, BlockBreaker>()
    private val internalBreakers = HashMap<Int, VisibleBreakMethod>()
    
    fun init() {
        registerEvents()
        registerPacketListener()
        runTaskTimer(0, 1, BlockBreaking::handleTick)
    }
    
    internal fun setBreakStage(pos: BlockPos, entityId: Int, stage: Int) {
        val blockState = BlockManager.getBlock(pos) ?: return
        
        val block = pos.block
        var method = internalBreakers[entityId]
        
        // check that this is a valid stage, otherwise remove the current break effect
        if (stage !in 0..9) {
            method?.stop()
            internalBreakers -= entityId
            return
        }
        
        // check that the previous break effect is on that block, otherwise cancel the previous effect
        if (method != null && method.pos != blockState.pos) {
            method.stop()
            method = null
            internalBreakers -= entityId
        }
        
        // create a new break method if there isn't one
        if (method == null) {
            method = BreakMethod.of(block, blockState.material, entityId) as? VisibleBreakMethod ?: return
            internalBreakers[entityId] = method
        }
        
        // set the break stage
        method.breakStage = stage
    }
    
    internal fun setBreakCooldown(player: Player) {
        breakCooldowns[player] = serverTick + BREAK_COOLDOWN
    }
    
    private fun handleTick() {
        playerBreakers.removeIf { (_, breaker) ->
            try {
                if (!breaker.isStopped)
                    breaker.handleTick()
            } catch (e: Exception) {
                LOGGER.log(Level.SEVERE, "An exception occurred in BlockBreaker tick", e)
            }
            
            return@removeIf breaker.isStopped
        }
    }
    
    private fun handleDestroyStart(player: Player, packet: ServerboundPlayerActionPacket, pos: BlockPos, direction: Direction, sequence: Int) {
        val block = pos.block
        
        // pass packet further down the pipeline if the block is from a custom item service
        if (CustomItemServiceManager.getBlockType(block) != null) {
            player.packetHandler?.injectIncoming(packet)
            return
        }
        
        val serverPlayer = player.serverPlayer
        
        // pass packet to vanilla packet handler if the player is in creative mode
        if (player.gameMode == GameMode.CREATIVE) {
            serverPlayer.connection.handlePlayerAction(packet)
        }
        
        // call interact event
        val event = CraftEventFactory.callPlayerInteractEvent(
            serverPlayer,
            Action.LEFT_CLICK_BLOCK,
            pos.nmsPos,
            direction,
            serverPlayer.inventory.getSelected(),
            InteractionHand.MAIN_HAND
        )
        if (event.useInteractedBlock() == Event.Result.DENY) {
            player.send(ClientboundBlockChangedAckPacket(sequence))
            return
        }
        
        // call block state attack (i.e. teleport dragon egg, play note block sound...)
        if (player.gameMode != GameMode.CREATIVE) {
            val serverLevel = pos.world.serverLevel
            val nmsPos = pos.nmsPos
            serverLevel.getBlockState(nmsPos).attack(serverLevel, nmsPos, player.serverPlayer)
        }
        
        // start breaker
        val novaBlockState = BlockManager.getBlock(pos)
        val breaker = if (novaBlockState != null)
            NovaBlockBreaker(player, block, novaBlockState, sequence, breakCooldowns[player] ?: 0)
        else VanillaBlockBreaker(player, block, sequence, breakCooldowns[player] ?: 0)
        
        playerBreakers[player] = breaker
        breaker.handleTick()
    }
    
    private fun handleDestroyAbort(player: Player, packet: ServerboundPlayerActionPacket) {
        val breaker = playerBreakers.remove(player)
        if (breaker != null) {
            breaker.stop(packet.sequence)
        } else {
            player.packetHandler?.injectIncoming(packet)
        }
    }
    
    private fun handleDestroyStop(player: Player, packet: ServerboundPlayerActionPacket) {
        val breaker = playerBreakers.remove(player)
        if (breaker != null) {
            if (breaker.progress > 0.7) {
                breaker.breakBlock(true, packet.sequence)
                breaker.stop()
            } else {
                breaker.stop(packet.sequence)
            }
        } else {
            player.packetHandler?.injectIncoming(packet)
        }
    }
    
    @PacketHandler
    private fun handlePlayerAction(event: ServerboundPlayerActionPacketEvent) {
        val player = event.player
        val pos = event.pos
        val blockPos = BlockPos(event.player.world, pos.x, pos.y, pos.z)
        
        event.isCancelled = when (event.action) {
            START_DESTROY_BLOCK -> {
                runTask { handleDestroyStart(player, event.packet, blockPos, event.direction, event.sequence) }
                true
            }
            
            ABORT_DESTROY_BLOCK -> {
                runTask { handleDestroyAbort(player, event.packet) }
                true
            }
            
            STOP_DESTROY_BLOCK -> {
                runTask { handleDestroyStop(player, event.packet) }
                true
            }
            
            else -> false
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    private fun handleQuit(event: PlayerQuitEvent) {
        val player = event.player
        
        breakCooldowns -= player
        playerBreakers.remove(player)?.stop()
    }
    
}