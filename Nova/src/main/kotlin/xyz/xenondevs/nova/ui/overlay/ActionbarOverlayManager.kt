package xyz.xenondevs.nova.ui.overlay

import net.md_5.bungee.api.chat.BaseComponent
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.TextComponent
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.network.event.clientbound.ActionBarPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ChatPacketEvent
import xyz.xenondevs.nova.util.data.forceDefaultFont
import xyz.xenondevs.nova.util.data.toPlainText
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.util.send
import java.util.*

object ActionbarOverlayManager : Listener {
    
    private val EMPTY_ACTION_BAR_PACKET = ClientboundSetActionBarTextPacket(TextComponent(""))
    private val overlays = HashMap<UUID, HashSet<ActionbarOverlay>>()
    private val interceptedActionbars = HashMap<UUID, Pair<ArrayList<BaseComponent>, Long>>()
    
    init {
        if (DEFAULT_CONFIG.getBoolean("actionbar_overlay.enabled")) {
            Bukkit.getPluginManager().registerEvents(this, NOVA)
            runTaskTimer(0, 1, ::handleTick)
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
    
    @EventHandler
    private fun handleChatPacket(event: ChatPacketEvent) {
        if (event.chatType == ChatType.GAME_INFO) {
            val player = event.player
            val uuid = player.uniqueId
            if (overlays.containsKey(uuid)) {
                val message = event.message
                if (message != null)
                    saveInterceptedComponent(player, message)
                else interceptedActionbars -= uuid
                
                event.message = getCurrentText(player)
            }
        }
    }
    
    @EventHandler
    private fun handleChatPacket(event: ActionBarPacketEvent) {
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
    
    private fun saveInterceptedComponent(player: Player, text: Array<BaseComponent>) {
        val components = ArrayList<BaseComponent>()
        
        // calculate the length (in pixels) of the intercepted message
        val textLength = MoveCharacters.getStringLength(text.toPlainText(player.locale))
        // to center, move the cursor to the right by half of the length
        components.add(MoveCharacters.getMovingComponent(textLength / -2))
        // append the text while explicitly setting it to the default font (required because of the moving component)
        components.addAll(text.forceDefaultFont())
        // move half of the text length back so the cursor is in the middle of the screen again (prevents clientside centering)
        components.add(MoveCharacters.getMovingComponent(textLength / -2))
        
        interceptedActionbars[player.uniqueId] = components to System.currentTimeMillis()
    }
    
    private fun getCurrentText(player: Player): Array<BaseComponent> {
        val uuid = player.uniqueId
        val componentList = ArrayList<BaseComponent>()
        
        overlays[uuid]!!.forEach {
            // add text
            componentList.addAll(it.text)
            // move back
            componentList.add(MoveCharacters.getMovingComponent(-it.width))
        }
        
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