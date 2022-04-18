package xyz.xenondevs.nova.network

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerLoginEvent.Result
import org.bukkit.event.player.PlayerQuitEvent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.PLUGIN_MANAGER
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.util.channels
import xyz.xenondevs.nova.util.minecraftServer
import xyz.xenondevs.nova.util.serverPlayer

object PacketManager : Initializable(), Listener {
    
    override val inMainThread = true
    override val dependsOn = emptySet<Initializable>()
    
    private val serverChannels = ArrayList<Channel>()
    private val connectionsList = minecraftServer.connection!!.connections
    
    val playerHandlers = HashMap<String, PacketHandler>()
    
    override fun init() {
        LOGGER.info("Registering packet handlers")
        PLUGIN_MANAGER.registerEvents(this, NOVA)
        registerHandlers()
    }
    
    override fun disable() {
        LOGGER.info("Unregistering packet handlers")
        Bukkit.getOnlinePlayers().forEach(::unregisterHandler)
        serverChannels.forEach { channel ->
            channel.eventLoop().submit {
                val pipeline = channel.pipeline()
                pipeline.context("nova_pipeline_adapter")?.handler()?.run(pipeline::remove)
            }
        }
    }
    
    private fun registerHandlers() {
        minecraftServer.channels.forEach { future ->
            val channel = future.channel()
            serverChannels.add(channel)
            val pipeline = channel.pipeline()
            pipeline.context("nova_pipeline_adapter")?.handler()?.run(pipeline::remove)
            pipeline.addFirst("nova_pipeline_adapter", PipelineAdapter)
        }
        Bukkit.getOnlinePlayers().forEach { unregisterHandler(it); registerHandler(it) }
    }
    
    @EventHandler
    private fun handleLogin(event: PlayerLoginEvent) {
        val handler = playerHandlers[event.player.name]
        if (handler == null) {
            event.disallow(Result.KICK_OTHER, "[Nova] Something went wrong")
            return
        }
        handler.player = event.player
    }
    
    @EventHandler
    private fun handleQuit(event: PlayerQuitEvent) {
        playerHandlers -= event.player.name
    }
    
    object PipelineAdapter : ChannelInboundHandlerAdapter() {
        
        override fun channelRead(ctx: ChannelHandlerContext, msg: Any?) {
            if (msg is Channel)
                msg.pipeline().addFirst("nova_pre_init_handler", PreInitHandler)
            super.channelRead(ctx, msg)
        }
        
    }
    
    object PreInitHandler : ChannelInitializer<Channel>() {
        
        override fun initChannel(channel: Channel) {
            channel.pipeline().addLast("nova_init_handler", NovaInitHandler)
        }
        
    }
    
    object NovaInitHandler : ChannelInitializer<Channel>() {
        
        override fun initChannel(channel: Channel) {
            synchronized(connectionsList) {
                channel.eventLoop().submit {
                    channel.pipeline().addBefore("packet_handler", "nova_packet_handler", PacketHandler(channel))
                }
            }
        }
        
    }
    
    private fun registerHandler(player: Player) {
        val channel = player.serverPlayer.connection.connection.channel
        channel.pipeline().addBefore("packet_handler", "nova_packet_handler", PacketHandler(channel, player))
    }
    
    private fun unregisterHandler(player: Player) {
        val pipeline = player.serverPlayer.connection.connection.channel.pipeline()
        pipeline.context("nova_packet_handler")?.handler()?.run(pipeline::remove)
    }
    
}