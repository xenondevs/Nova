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
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.PLUGIN_MANAGER
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.util.channels
import xyz.xenondevs.nova.util.minecraftServer
import xyz.xenondevs.nova.util.serverPlayer

object PacketListener : Initializable(), Listener {
    
    override val inMainThread = true
    override val dependsOn = CustomItemServiceManager
    
    private val serverChannels = ArrayList<Channel>()
    private val connectionsList = minecraftServer.connection!!.connections
    
    val playerHandlers = HashMap<String, PacketHandler>()
    
    override fun init() {
        LOGGER.info("Initializing PacketListener")
        PLUGIN_MANAGER.registerEvents(this, NOVA)
        registerHandlers()
        NOVA.disableHandlers += {
            Bukkit.getOnlinePlayers().forEach(::unregisterHandler)
            serverChannels.forEach { channel ->
                channel.eventLoop().submit {
                    val pipeline = channel.pipeline()
                    pipeline.context("nova_pipeline_adapter")?.handler()?.run(pipeline::remove)
                }
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
    fun handleLogin(event: PlayerLoginEvent) {
        val uuid = event.player.name
        if (uuid !in playerHandlers) {
            event.disallow(Result.KICK_OTHER, "[Nova] Something went wrong")
            return
        }
        playerHandlers[uuid]!!.player = event.player
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
                    channel.pipeline().addBefore("packet_handler", "nova_packet_handler", PacketHandler())
                }
            }
        }
        
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