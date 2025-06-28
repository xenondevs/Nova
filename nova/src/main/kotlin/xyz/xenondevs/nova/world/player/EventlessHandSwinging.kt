package xyz.xenondevs.nova.world.player

import net.minecraft.network.protocol.game.ClientboundAnimatePacket
import net.minecraft.world.InteractionHand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.network.event.PacketHandler
import xyz.xenondevs.nova.network.event.PacketListener
import xyz.xenondevs.nova.network.event.registerPacketListener
import xyz.xenondevs.nova.network.event.serverbound.ServerboundSwingPacketEvent
import xyz.xenondevs.nova.util.decrementIfGreaterThanZero
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.serverPlayer
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Lets the [player][this] swing their main hand without triggering any server-side
 * interaction related events.
 */
fun Player.swingMainHandEventless() {
    val packet = ClientboundAnimatePacket(serverPlayer, 0)
    EventlessHandSwinging.registerDrop(this, true)
    world.serverLevel.chunkSource.broadcastAndSend(serverPlayer, packet)
}

/**
 * Lets the [player][this] swing their off-hand without triggering any server-side
 * interaction related events.
 */
fun Player.swingOffHandEventless() {
    val packet = ClientboundAnimatePacket(serverPlayer, 3)
    EventlessHandSwinging.registerDrop(this, false)
    world.serverLevel.chunkSource.broadcastAndSend(serverPlayer, packet)
}

/**
 * Lets the [player][this] swing their [hand] without triggering any server-side
 * interaction related events.
 * 
 * @throws IllegalArgumentException if the [hand] is not [EquipmentSlot.HAND] or [EquipmentSlot.OFF_HAND]
 */
fun Player.swingHandEventless(hand: EquipmentSlot) {
    when (hand) {
        EquipmentSlot.HAND -> swingMainHandEventless()
        EquipmentSlot.OFF_HAND -> swingOffHandEventless()
        else -> throw IllegalArgumentException("Invalid slot: $hand")
    }
}

@InternalInit(stage = InternalInitStage.POST_WORLD)
internal object EventlessHandSwinging : PacketListener, Listener {
    
    private val toDrop = ConcurrentHashMap<UUID, Pair<AtomicInteger, AtomicInteger>>()
    
    @InitFun
    private fun init() {
        registerPacketListener()
        registerEvents()
    }
    
    @PacketHandler
    private fun handleSwingPacket(event: ServerboundSwingPacketEvent) {
        val (main, off) = toDrop[event.player.uniqueId] ?: return
        if (event.hand == InteractionHand.MAIN_HAND && main.decrementIfGreaterThanZero()) {
            event.isCancelled = true
        } else if (event.hand == InteractionHand.OFF_HAND && off.decrementIfGreaterThanZero()) {
            event.isCancelled = true
        }
    }
    
    fun registerDrop(player: Player, mainHand: Boolean) {
        val (main, off) = toDrop.computeIfAbsent(player.uniqueId) { AtomicInteger() to AtomicInteger() }
        if (mainHand) {
            main.incrementAndGet()
        } else {
            off.incrementAndGet()
        }
    }
    
    @EventHandler
    private fun handleQuit(event: PlayerQuitEvent) {
        toDrop.remove(event.player.uniqueId)
    }
    
}