package xyz.xenondevs.nmsutils.network

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import net.minecraft.network.FriendlyByteBuf
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerLoginEvent.Result
import org.bukkit.event.player.PlayerQuitEvent
import xyz.xenondevs.nmsutils.LOGGER
import xyz.xenondevs.nmsutils.PLUGIN
import xyz.xenondevs.nmsutils.internal.util.DEDICATED_SERVER
import xyz.xenondevs.nmsutils.internal.util.channels
import xyz.xenondevs.nmsutils.internal.util.connection
import xyz.xenondevs.nmsutils.internal.util.serverPlayer

fun Player.send(vararg bufs: FriendlyByteBuf, retain: Boolean = true, flush: Boolean = true) {
    val queue = PacketManager.playerHandlers[name]?.queue ?: return
    bufs.forEach {
        if (retain) it.retain()
        queue += it
    }
    
    if (flush) connection.connection.channel.flush()
}

internal object PacketManager : Listener {
    
    private lateinit var serverChannel: Channel
    private val connectionsList = DEDICATED_SERVER.connection!!.connections
    
    val playerHandlers = HashMap<String, PacketHandler>()
    
    fun init() {
        LOGGER.info("Registering packet handlers")
        Bukkit.getPluginManager().registerEvents(this, PLUGIN)
        registerHandlers()
    }
    
    fun disable() {
        LOGGER.info("Unregistering packet handlers")
        Bukkit.getOnlinePlayers().forEach(::unregisterHandler)
        
        if (::serverChannel.isInitialized) {
            serverChannel.eventLoop().submit {
                val pipeline = serverChannel.pipeline()
                pipeline.context("${PLUGIN.name}_pipeline_adapter")?.handler()?.run(pipeline::remove)
            }
        }
    }
    
    private fun registerHandlers() {
        serverChannel = DEDICATED_SERVER.channels.first().channel()
        
        val pipeline = serverChannel.pipeline()
        pipeline.context("${PLUGIN.name}_pipeline_adapter")?.handler()?.run(pipeline::remove)
        pipeline.addFirst("${PLUGIN.name}_pipeline_adapter", PipelineAdapter)
        
        Bukkit.getOnlinePlayers().forEach { unregisterHandler(it); registerHandler(it) }
    }
    
    @EventHandler
    private fun handleLogin(event: PlayerLoginEvent) {
        val handler = playerHandlers[event.player.name]
        if (handler == null) {
            event.disallow(Result.KICK_OTHER, "[${PLUGIN.name}] Something went wrong")
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
                msg.pipeline().addFirst("${PLUGIN.name}_pre_init_handler", PreInitHandler)
            super.channelRead(ctx, msg)
        }
        
    }
    
    object PreInitHandler : ChannelInitializer<Channel>() {
        
        override fun initChannel(channel: Channel) {
            channel.pipeline().addLast("${PLUGIN.name}_init_handler", NovaInitHandler)
        }
        
    }
    
    object NovaInitHandler : ChannelInitializer<Channel>() {
        
        override fun initChannel(channel: Channel) {
            synchronized(connectionsList) {
                channel.eventLoop().submit {
                    channel.pipeline().addBefore("packet_handler", "${PLUGIN.name}_packet_handler", PacketHandler(channel))
                }
            }
        }
        
    }
    
    private fun registerHandler(player: Player) {
        val channel = player.serverPlayer.connection.connection.channel
        channel.pipeline().addBefore("packet_handler", "${PLUGIN.name}_packet_handler", PacketHandler(channel, player))
    }
    
    private fun unregisterHandler(player: Player) {
        val pipeline = player.serverPlayer.connection.connection.channel.pipeline()
        pipeline.context("${PLUGIN.name}_packet_handler")?.handler()?.run(pipeline::remove)
    }
    
}