package xyz.xenondevs.nova.world.block.logic.`break`

import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket.AttributeSnapshot
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.*
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.ai.attributes.Attributes
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.craftbukkit.event.CraftEventFactory
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import xyz.xenondevs.commons.collections.removeIf
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.DefaultContextIntentions.BlockBreak
import xyz.xenondevs.nova.context.param.DefaultContextParamTypes
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.network.event.PacketHandler
import xyz.xenondevs.nova.network.event.PacketListener
import xyz.xenondevs.nova.network.event.clientbound.ClientboundUpdateAttributesPacketEvent
import xyz.xenondevs.nova.network.event.registerPacketListener
import xyz.xenondevs.nova.network.event.serverbound.ServerboundPlayerActionPacketEvent
import xyz.xenondevs.nova.network.packetHandler
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.util.send
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.util.serverTick
import xyz.xenondevs.nova.util.toNovaPos
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.behavior.Breakable
import xyz.xenondevs.nova.world.format.WorldDataManager
import java.util.concurrent.ConcurrentHashMap

private val BREAK_COOLDOWN by MAIN_CONFIG.entry<Int>("world", "block_breaking", "break_cooldown")

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dependsOn = [WorldDataManager::class]
)
internal object BlockBreaking : Listener, PacketListener {
    
    private val breakCooldowns = ConcurrentHashMap<Player, Int>()
    private val playerBreakers = ConcurrentHashMap<Player, BlockBreaker>()
    private val internalBreakers = HashMap<Int, VisibleBreakMethod>()
    
    @InitFun
    private fun init() {
        registerEvents()
        registerPacketListener()
        runTaskTimer(0, 1, BlockBreaking::handleTick)
    }
    
    fun setBreakStage(pos: BlockPos, entityId: Int, stage: Int) {
        val blockState = WorldDataManager.getBlockState(pos) ?: return
        
        val block = pos.block
        var method = internalBreakers[entityId]
        
        // check that this is a valid stage, otherwise remove the current break effect
        if (stage !in 0..9) {
            method?.stop()
            internalBreakers -= entityId
            return
        }
        
        // check that the previous break effect is on that block, otherwise cancel the previous effect
        if (method != null && method.pos != pos) {
            method.stop()
            method = null
            internalBreakers -= entityId
        }
        
        // create a new break method if there isn't one
        if (method == null) {
            method = BreakMethod.of(block, blockState.block, entityId) as? VisibleBreakMethod ?: return
            internalBreakers[entityId] = method
        }
        
        // set the break stage
        method.breakStage = stage
    }
    
    fun setBreakCooldown(player: Player) {
        breakCooldowns[player] = serverTick + BREAK_COOLDOWN
    }
    
    fun getBreaker(player: Player): BlockBreaker? {
        return playerBreakers[player]
    }
    
    private fun handleTick() {
        playerBreakers.removeIf { (_, breaker) ->
            try {
                if (!breaker.isStopped)
                    breaker.handleTick()
            } catch (e: Exception) {
                LOGGER.error("An exception occurred in BlockBreaker tick", e)
            }
            
            return@removeIf breaker.isStopped
        }
    }
    
    private fun handleDestroyStart(player: Player, packet: ServerboundPlayerActionPacket, pos: BlockPos, direction: Direction, sequence: Int) {
        // pass packet further down the pipeline if:
        // - the block is from a custom item service
        // - the player is using a debug stick
        if (CustomItemServiceManager.getBlockType(pos.block) != null
            || (player.gameMode == GameMode.CREATIVE && player.inventory.itemInMainHand.type == Material.DEBUG_STICK)
        ) {
            player.packetHandler?.injectIncoming(packet)
            return
        }
        
        val serverPlayer = player.serverPlayer
        
        // call interact event
        val event = CraftEventFactory.callPlayerInteractEvent(
            serverPlayer,
            Action.LEFT_CLICK_BLOCK,
            pos.nmsPos,
            direction,
            serverPlayer.inventory.selectedItem,
            InteractionHand.MAIN_HAND
        )
        if (event.useInteractedBlock() == Event.Result.DENY) {
            player.send(ClientboundBlockChangedAckPacket(sequence))
            return
        }
        
        // call block state attack (e.g. teleport dragon egg, play note block sound...)
        val novaBlockState = WorldDataManager.getBlockState(pos)
        if (player.gameMode != GameMode.CREATIVE) {
            if (novaBlockState != null) {
                val ctx = Context.intention(BlockBreak)
                    .param(DefaultContextParamTypes.BLOCK_POS, pos)
                    .param(DefaultContextParamTypes.BLOCK_STATE_NOVA, novaBlockState)
                    .param(DefaultContextParamTypes.SOURCE_PLAYER, player)
                    .build()
                novaBlockState.block.handleAttack(pos, novaBlockState, ctx)
            } else {
                val serverLevel = pos.world.serverLevel
                val nmsPos = pos.nmsPos
                serverLevel.getBlockState(nmsPos).attack(serverLevel, nmsPos, player.serverPlayer)
            }
        }
        
        // start breaker
        val breaker: BlockBreaker
        if (novaBlockState != null) {
            // don't do any breaking logic if the block doesn't have the breakable behavior
            if (!novaBlockState.block.hasBehavior<Breakable>()) {
                player.send(ClientboundBlockChangedAckPacket(sequence))
                return
            }
            
            breaker = NovaBlockBreaker(player, pos, novaBlockState, sequence, breakCooldowns[player] ?: 0)
        } else {
            breaker = VanillaBlockBreaker(player, pos, sequence, breakCooldowns[player] ?: 0)
        }
        
        // creative breakers should not be added to the playerBreakers map because players in creative mode
        // do not send an abort or stop packet and therefore the breaker would never be removed
        if (player.gameMode != GameMode.CREATIVE) {
            playerBreakers[player] = breaker
        }
        
        // handle initial tick
        breaker.handleTick()
    }
    
    private fun handleDestroyAbort(player: Player, packet: ServerboundPlayerActionPacket) {
        val breaker = playerBreakers.remove(player)
        if (breaker != null) {
            breaker.stop(false, packet.sequence)
        } else {
            player.packetHandler?.injectIncoming(packet)
        }
    }
    
    private fun handleDestroyStop(player: Player, packet: ServerboundPlayerActionPacket) {
        val breaker = playerBreakers.remove(player)
        if (breaker != null) {
            breaker.breakBlock(true, packet.sequence)
            breaker.stop(true)
        } else {
            player.packetHandler?.injectIncoming(packet)
        }
    }
    
    @PacketHandler
    private fun handlePlayerAction(event: ServerboundPlayerActionPacketEvent) {
        val player = event.player
        val pos = event.pos.toNovaPos(player.world)
        
        event.isCancelled = when (event.action) {
            START_DESTROY_BLOCK -> {
                runTask { handleDestroyStart(player, event.packet, pos, event.direction, event.sequence) }
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
    
    @PacketHandler
    private fun handleAttributes(event: ClientboundUpdateAttributesPacketEvent) {
        if (event.player.entityId != event.entityId)
            return
        
        event.values = event.values.map {
            if (it.attribute.value() == Attributes.BLOCK_BREAK_SPEED.value()) {
                AttributeSnapshot(it.attribute, 0.0, emptyList())
            } else {
                it
            }
        }
    }
    
    @EventHandler
    private fun handleJoin(event: PlayerJoinEvent) {
        val player = event.player
        val attributes = listOf(AttributeSnapshot(Attributes.BLOCK_BREAK_SPEED, 0.0, emptyList()))
        val packet = ClientboundUpdateAttributesPacket(player.entityId, attributes)
        player.send(packet)
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    private fun handleQuit(event: PlayerQuitEvent) {
        val player = event.player
        
        breakCooldowns -= player
        playerBreakers.remove(player)?.stop(false)
    }
    
}