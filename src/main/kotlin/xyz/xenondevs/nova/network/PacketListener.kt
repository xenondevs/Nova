package xyz.xenondevs.nova.network

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.util.serverPlayer

object PacketListener : Listener {
    
    fun init() {
        LOGGER.info("Initializing PacketListener")
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
        Bukkit.getOnlinePlayers().forEach { unregisterHandler(it); registerHandler(it) }
        NOVA.disableHandlers += { Bukkit.getOnlinePlayers().forEach(::unregisterHandler) }
    }
    
    @EventHandler
    fun handlePlayerJoin(event: PlayerJoinEvent) {
        registerHandler(event.player)
    }
    
    private fun registerHandler(player: Player) {
        val pipeline = player.serverPlayer.connection.connection.channel.pipeline()
        pipeline.addBefore("packet_handler", "nova_packet_handler", PacketHandler(player))
    }
    
    private fun unregisterHandler(player: Player) {
        val pipeline = player.serverPlayer.connection.connection.channel.pipeline()
        pipeline.context("nova_packet_handler")?.handler()?.run(pipeline::remove)
    }
    
}