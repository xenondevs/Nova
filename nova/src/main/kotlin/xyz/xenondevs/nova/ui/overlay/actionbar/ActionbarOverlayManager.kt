package xyz.xenondevs.nova.ui.overlay.actionbar

import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.network.event.PacketHandler
import xyz.xenondevs.nova.network.event.PacketListener
import xyz.xenondevs.nova.network.event.clientbound.ClientboundActionBarPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSystemChatPacketEvent
import xyz.xenondevs.nova.network.event.registerPacketListener
import xyz.xenondevs.nova.network.event.unregisterPacketListener
import xyz.xenondevs.nova.resources.CharSizes
import xyz.xenondevs.nova.util.component.adventure.move
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.util.send
import java.util.*
import net.minecraft.network.chat.Component as MojangComponent

object ActionbarOverlayManager : PacketListener {
    
    private var tickTask: BukkitTask? = null
    
    private val EMPTY_ACTION_BAR_PACKET = ClientboundSetActionBarTextPacket(MojangComponent.empty())
    private val overlays = HashMap<UUID, HashSet<ActionbarOverlay>>()
    private val interceptedActionbars = HashMap<UUID, Pair<Component, Long>>()
    
    init {
        val enabled = MAIN_CONFIG.entry<Boolean>("overlay", "actionbar", "enabled")
        enabled.subscribe(::reload)
        enabled.update()
    }
    
    private fun reload(enabled: Boolean) {
        if (tickTask != null) {
            tickTask?.cancel()
            unregisterPacketListener()
            tickTask = null
        }
        
        if (enabled) {
            registerPacketListener()
            tickTask = runTaskTimer(0, 1, ActionbarOverlayManager::handleTick)
        }
    }
    
    fun registerOverlay(player: Player, overlay: ActionbarOverlay) {
        overlays.getOrPut(player.uniqueId) { HashSet() } += overlay
    }
    
    fun unregisterOverlay(player: Player, overlay: ActionbarOverlay) {
        val playerOverlays = overlays[player.uniqueId]
        if (playerOverlays != null) {
            playerOverlays -= overlay
            if (playerOverlays.isEmpty()) {
                overlays.remove(player.uniqueId)
                player.send(EMPTY_ACTION_BAR_PACKET)
            }
        }
    }
    
    /**
     * Every tick, we send empty actionbar packets to all players in the [overlays] map.
     * These packets will the get intercepted in the [handleChatPacket] where we can append
     * the characters from the [ActionbarOverlay].
     * We do it this way, so we can overwrite all actionbar messages and prevent flickering
     * between two different plugins trying to send their actionbar.
     */
    private fun handleTick() {
        overlays.keys
            .asSequence()
            .mapNotNull(Bukkit::getPlayer)
            .forEach { it.send(EMPTY_ACTION_BAR_PACKET) }
    }
    
    @PacketHandler
    private fun handleChatPacket(event: ClientboundSystemChatPacketEvent) {
        if (event.overlay) {
            val player = event.player
            val uuid = player.uniqueId
            if (overlays.containsKey(uuid)) {
                saveInterceptedComponent(player, event.message)
                event.message = getCurrentText(player)
            }
        }
    }
    
    @PacketHandler
    private fun handleChatPacket(event: ClientboundActionBarPacketEvent) {
        val player = event.player
        val uuid = player.uniqueId
        if (overlays.containsKey(uuid)) {
            if (event.packet !== EMPTY_ACTION_BAR_PACKET) {
                saveInterceptedComponent(player, event.text)
            }
            
            event.text = getCurrentText(player)
        }
    }
    
    @Suppress("DEPRECATION")
    private fun saveInterceptedComponent(player: Player, text: Component) {
        val mv = CharSizes.calculateComponentWidth(text, player.locale) / -2
        
        val component = Component.text()
            .move(mv) // to center, move the cursor to the right by half of the length
            .append(text)
            .move(mv) // move half of the text length back so the cursor is in the middle of the screen again (prevents client-side centering)
            .build()
        
        interceptedActionbars[player.uniqueId] = component to System.currentTimeMillis()
    }
    
    @Suppress("DEPRECATION")
    private fun getCurrentText(player: Player): Component {
        val uuid = player.uniqueId
        val builder = Component.text()
        
        // append custom overlays
        overlays[uuid]!!.forEach {
            builder.append(it.component)
            builder.move(-it.getWidth(player.locale))
        }
        
        // append intercepted actionbar text
        val interceptedActionbar = interceptedActionbars[uuid]
        if (interceptedActionbar != null) {
            val (text, time) = interceptedActionbar
            if (System.currentTimeMillis() - time < 3000) {
                builder.append(text)
            } else interceptedActionbars -= uuid
        }
        
        return builder.build()
    }
    
}