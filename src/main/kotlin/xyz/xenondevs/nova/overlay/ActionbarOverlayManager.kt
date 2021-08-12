package xyz.xenondevs.nova.overlay

import net.md_5.bungee.api.chat.BaseComponent
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.packet.event.impl.ClientboundActionBarPacketEvent
import xyz.xenondevs.nova.packet.event.impl.ClientboundChatPacketEvent
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.util.send

// TODO: This currently only works with one overlay due to the centering of the text
object ActionbarOverlayManager : Listener {
    
    private val EMPTY_ACTION_BAR_PACKET = ClientboundSetActionBarTextPacket(null as Component?)
    private val overlays = HashMap<Player, HashSet<ActionbarOverlay>>()
    
    init {
        Bukkit.getPluginManager().registerEvents(this, NOVA)
        runTaskTimer(0, 1, ::handleTick)
    }
    
    fun registerOverlay(player: Player, overlay: ActionbarOverlay) {
        overlays.getOrPut(player) { HashSet() } += overlay
    }
    
    fun unregisterOverlay(player: Player, overlay: ActionbarOverlay) {
        overlays[player]?.remove(overlay)
    }
    
    /**
     * Every tick, we send empty actionbar packets to all players in the [overlays] map.
     * These packets will the get intercepted in the [handleChatPacket] where we can append
     * the characters from the [ActionbarOverlay].
     * We do it this way, so we can overwrite all actionbar messages and prevent flickering
     * between two different plugins trying to send their actionbar.
     */
    private fun handleTick() {
        overlays.forEach { (player, _) -> player.send(EMPTY_ACTION_BAR_PACKET) }
    }
    
    @EventHandler
    private fun handleChatPacket(event: ClientboundChatPacketEvent) {
        if (event.chatType == ChatType.GAME_INFO) {
            val player = event.player
            if (overlays.containsKey(player))
                event.message = getCurrentText(player)
            else if (event.message == null)
                event.isCancelled = true
        }
    }
    
    @EventHandler
    private fun handleChatPacket(event: ClientboundActionBarPacketEvent) {
        val player = event.player
        if (overlays.containsKey(player))
            event.text = getCurrentText(player)
        else if (event.text == null)
            event.isCancelled = true
    }
    
    private fun getCurrentText(player: Player): Array<BaseComponent> {
        val componentList = ArrayList<BaseComponent>()
        overlays[player]!!.forEach { componentList.addAll(it.text) }
        return componentList.toTypedArray()
    }
    
}