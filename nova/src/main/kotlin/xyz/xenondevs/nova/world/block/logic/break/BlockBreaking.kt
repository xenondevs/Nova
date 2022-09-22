package xyz.xenondevs.nova.world.block.logic.`break`

import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.*
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.network.event.PacketEventManager
import xyz.xenondevs.nmsutils.network.event.PacketHandler
import xyz.xenondevs.nmsutils.network.event.serverbound.ServerboundPlayerActionPacketEvent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.util.concurrent.runInServerThread
import xyz.xenondevs.nova.util.hardness
import xyz.xenondevs.nova.util.item.takeUnlessAir
import xyz.xenondevs.nova.util.nmsPos
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

private const val BREAK_COOLDOWN = 5

internal object BlockBreaking {
    
    private val breakCooldowns = ConcurrentHashMap<Player, Int>()
    private val playerBreakers = ConcurrentHashMap<Player, BlockBreaker>()
    private val internalBreakers = HashMap<Int, BreakMethod>()
    
    fun init() {
        PacketEventManager.registerListener(this)
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
            method = BreakMethod.of(block, blockState.material, entityId) ?: return
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
                breaker.handleTick()
            } catch (e: Exception) {
                LOGGER.log(Level.SEVERE, "An exception occurred in BlockBreaker tick", e)
            }
            
            return@removeIf breaker.isStopped
        }
    }
    
    private fun handleDestroyStart(player: Player, pos: BlockPos, sequence: Int): Boolean {
        // TODO: do not run if block is from other CustomItemService
        val block = pos.block
        if (block.hardness >= 0) {
            // server thread - accessing block states
            runTask {
                // call block state attack (i.e. teleport dragon egg, play note block sound...)
                val serverLevel = pos.world.serverLevel
                val nmsPos = pos.nmsPos
                serverLevel.getBlockState(nmsPos).attack(serverLevel, nmsPos, player.serverPlayer)
            }
            
            // check protection integrations, then start breaker if allowed
            val future = ProtectionManager.canBreak(player, player.inventory.itemInMainHand.takeUnlessAir(), pos.location)
            future.thenRun {
                val result = future.get()
                if (result) {
                    // server thread - accessing block states
                    runInServerThread {
                        val novaBlockState = BlockManager.getBlock(pos)
                        val breaker = if (novaBlockState != null) 
                            NovaBlockBreaker(player, block, novaBlockState, sequence, breakCooldowns[player] ?: 0)
                         else VanillaBlockBreaker(player, block, sequence, breakCooldowns[player] ?: 0)
                        
                        playerBreakers[player] = breaker
                    }
                } else {
                    // The ack packet removes client-predicted block states and shows those sent by the server
                    player.send(ClientboundBlockChangedAckPacket(sequence))
                }
            }
            
            return true
        }
        
        return false
    }
    
    private fun handleDestroyStop(player: Player): Boolean {
        val breaker = playerBreakers.remove(player)
        if (breaker != null) {
            breaker.stop()
            return true
        }
        return false
    }
    
    @PacketHandler
    private fun handlePlayerAction(event: ServerboundPlayerActionPacketEvent) {
        val player = event.player
        val pos = event.pos
        val blockPos = BlockPos(event.player.world, pos.x, pos.y, pos.z)
        
        event.isCancelled = when (event.action) {
            START_DESTROY_BLOCK -> handleDestroyStart(player, blockPos, event.sequence)
            STOP_DESTROY_BLOCK, ABORT_DESTROY_BLOCK -> handleDestroyStop(player)
            else -> false
        }
    }
    
}