package xyz.xenondevs.nova.world.player

import net.minecraft.network.protocol.game.ClientboundAnimatePacket
import net.minecraft.world.InteractionHand
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.plugin.java.JavaPlugin
import xyz.xenondevs.nova.network.event.PacketHandler
import xyz.xenondevs.nova.network.event.PacketListener
import xyz.xenondevs.nova.network.event.registerPacketListener
import xyz.xenondevs.nova.network.event.serverbound.ServerboundSwingPacketEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Lets the [player][this] swing their main hand without triggering any server-side
 * interaction related events.
 */
fun LivingEntity.swingMainHandEventless() {
    if (this is Player) {
        val serverPlayer = (this as CraftPlayer).handle
        val packet = ClientboundAnimatePacket(serverPlayer, 0)
        EventlessHandSwinging.registerDrop(this, true)
        (world as CraftWorld).handle.chunkSource.sendToTrackingPlayersAndSelf(serverPlayer, packet)
    } else {
        swingMainHand()
    }
}

/**
 * Lets the [player][this] swing their off-hand without triggering any server-side
 * interaction related events.
 */
fun LivingEntity.swingOffHandEventless() {
    if (this is Player) {
        val serverPlayer = (this as CraftPlayer).handle
        val packet = ClientboundAnimatePacket(serverPlayer, 3)
        EventlessHandSwinging.registerDrop(this, false)
        (world as CraftWorld).handle.chunkSource.sendToTrackingPlayersAndSelf(serverPlayer, packet)
    } else {
        swingOffHand()
    }
}

/**
 * Lets the [player][this] swing their [hand] without triggering any server-side
 * interaction related events.
 *
 * @throws IllegalArgumentException if the [hand] is not [EquipmentSlot.HAND] or [EquipmentSlot.OFF_HAND]
 */
fun LivingEntity.swingHandEventless(hand: EquipmentSlot) {
    when (hand) {
        EquipmentSlot.HAND -> swingMainHandEventless()
        EquipmentSlot.OFF_HAND -> swingOffHandEventless()
        else -> throw IllegalArgumentException("Invalid slot: $hand")
    }
}

private object EventlessHandSwinging : PacketListener, Listener {
    
    private val toDrop = ConcurrentHashMap<UUID, Pair<AtomicInteger, AtomicInteger>>()
    
    init {
        registerPacketListener()
        Bukkit.getPluginManager().registerEvents(
            this,
            JavaPlugin.getProvidingPlugin(EventlessHandSwinging::class.java)
        )
    }
    
    @PacketHandler
    private fun handleSwingPacket(event: ServerboundSwingPacketEvent) {
        val [main, off] = toDrop[event.player.uniqueId] ?: return
        if (event.hand == InteractionHand.MAIN_HAND && main.decrementIfGreaterThanZero()) {
            event.isCancelled = true
        } else if (event.hand == InteractionHand.OFF_HAND && off.decrementIfGreaterThanZero()) {
            event.isCancelled = true
        }
    }
    
    fun registerDrop(player: Player, mainHand: Boolean) {
        val [main, off] = toDrop.computeIfAbsent(player.uniqueId) { AtomicInteger() to AtomicInteger() }
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

private fun AtomicInteger.decrementIfGreaterThanZero(): Boolean {
    while (true) {
        val current = get()
        if (current <= 0)
            return false
        if (compareAndSet(current, current - 1))
            return true
    }
}
