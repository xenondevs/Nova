package xyz.xenondevs.nova.ui.overlay.actionbar

import net.md_5.bungee.api.chat.BaseComponent
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import xyz.xenondevs.nmsutils.network.event.PacketEventManager
import xyz.xenondevs.nmsutils.network.event.PacketHandler
import xyz.xenondevs.nmsutils.network.event.clientbound.ClientboundActionBarPacketEvent
import xyz.xenondevs.nmsutils.network.event.clientbound.ClientboundSystemChatPacketEvent
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.ui.overlay.character.DefaultFont
import xyz.xenondevs.nova.ui.overlay.character.MoveCharacters
import xyz.xenondevs.nova.util.component.bungee.forceDefaultFont
import xyz.xenondevs.nova.util.component.bungee.toPlainText
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.util.send
import java.util.*

object ActionbarOverlayManager {
    
    private var tickTask: BukkitTask? = null
    
    private val EMPTY_ACTION_BAR_PACKET = ClientboundSetActionBarTextPacket(Component.empty())
    private val overlays = HashMap<UUID, HashSet<ActionbarOverlay>>()
    private val interceptedActionbars = HashMap<UUID, Pair<ArrayList<BaseComponent>, Long>>()
    
    init {
        reload()
    }
    
    internal fun reload() {
        if (tickTask != null) {
            tickTask?.cancel()
            PacketEventManager.unregisterListener(this)
            tickTask = null
        }
        
        if (DEFAULT_CONFIG.getBoolean("overlay.actionbar.enabled")) {
            PacketEventManager.registerListener(this)
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
                val message = event.message
                if (message.isNotEmpty())
                    saveInterceptedComponent(player, message)
                else interceptedActionbars -= uuid
                
                event.message = getCurrentText(player)
            }
        }
    }
    
    @PacketHandler
    private fun handleChatPacket(event: ClientboundActionBarPacketEvent) {
        val player = event.player
        val uuid = player.uniqueId
        if (overlays.containsKey(uuid)) {
            val text = event.text
            if (event.packet !== EMPTY_ACTION_BAR_PACKET) {
                if (text != null)
                    saveInterceptedComponent(player, text)
                else interceptedActionbars -= uuid
            }
            
            event.text = getCurrentText(player)
        }
    }
    
    private fun saveInterceptedComponent(player: Player, text: Array<out BaseComponent>) {
        val components = ArrayList<BaseComponent>()
        
        // calculate the length (in pixels) of the intercepted message
        val textLength = DefaultFont.getStringLength(text.toPlainText(player.locale))
        // to center, move the cursor to the right by half of the length
        components.add(MoveCharacters.getMovingBungeeComponent(textLength / -2))
        // append the text while explicitly setting it to the default font (required because of the moving component)
        components.addAll(text.forceDefaultFont())
        // move half of the text length back so the cursor is in the middle of the screen again (prevents clientside centering)
        components.add(MoveCharacters.getMovingBungeeComponent(textLength / -2))
        
        interceptedActionbars[player.uniqueId] = components to System.currentTimeMillis()
    }
    
    private fun getCurrentText(player: Player): Array<BaseComponent> {
        val uuid = player.uniqueId
        val componentList = ArrayList<BaseComponent>()
        
        // TODO: reimplement
        
//        overlays[uuid]!!.forEach {
//            val components = it.components
//            // add components
//            componentList.addAll(components)
//            // move back
//            componentList.add(MoveCharacters.getMovingBungeeComponent(-it.getWidth(player.locale)))
//        }
        
        val interceptedActionbar = interceptedActionbars[uuid]
        if (interceptedActionbar != null) {
            val (text, time) = interceptedActionbar
            if (System.currentTimeMillis() - time < 3000) {
                componentList.addAll(text)
            } else interceptedActionbars -= uuid
        }
        
        return componentList.toTypedArray()
    }
    
}