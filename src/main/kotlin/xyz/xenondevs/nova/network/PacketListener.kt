package xyz.xenondevs.nova.network

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.util.serverPlayer

object PacketListener : Listener {
    
    fun init() {
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
        Bukkit.getOnlinePlayers().forEach(::registerHandler)
        NOVA.disableHandlers += { Bukkit.getOnlinePlayers().forEach(::unregisterHandlers)}
    }
    
    @EventHandler
    fun handlePlayerJoin(event: PlayerJoinEvent) {
        registerHandler(event.player)
    }
    
    private fun registerHandler(player: Player) {
        val pipeline = player.serverPlayer.connection.connection.channel.pipeline()
        pipeline.addBefore("packet_handler", "nova_packet_handler", PacketHandler(player))
    }
    
    private fun unregisterHandlers(player: Player) {
        val pipeline = player.serverPlayer.connection.connection.channel.pipeline()
        pipeline.remove("nova_packet_handler")
        println(pipeline.names().joinToString())
    }
    
}