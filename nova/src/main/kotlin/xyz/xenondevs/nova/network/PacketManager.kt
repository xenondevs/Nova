package xyz.xenondevs.nova.network

import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import net.kyori.adventure.text.Component
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.network.ServerConnectionListener
import net.minecraft.server.network.ServerLoginPacketListenerImpl
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.util.MINECRAFT_SERVER
import xyz.xenondevs.nova.util.registerEvents
import java.lang.invoke.MethodHandles
import java.util.*
import net.minecraft.world.entity.player.Player as MojangPlayer

private val SERVER_CONNECTION_LISTENER_CHANNELS_GETTER = MethodHandles
    .privateLookupIn(ServerConnectionListener::class.java, MethodHandles.lookup())
    .findGetter(ServerConnectionListener::class.java, "channels", List::class.java)

val Player.packetHandler: PacketHandler?
    get() = PacketManager.handlers[this]

val MojangPlayer.packetHandler: PacketHandler?
    get() = PacketManager.handlers[bukkitEntity as Player]

fun Player.send(vararg bufs: FriendlyByteBuf, retain: Boolean = true, flush: Boolean = true) {
    val packetHandler = packetHandler ?: return
    bufs.forEach {
        if (retain) it.retain()
        packetHandler.queueByteBuf(it)
    }
    
    if (flush) packetHandler.channel.flush()
}

private const val INIT_HANDLER_NAME = "nova_init"
private const val PACKET_HANDLER_NAME = "nova_packet_handler"

@InternalInit(stage = InternalInitStage.POST_WORLD)
internal object PacketManager : Listener {
    
    val handlers = WeakHashMap<Player, PacketHandler>()
    
    @Suppress("UNCHECKED_CAST")
    @InitFun
    private fun init() {
        registerEvents()
        
        val channels = SERVER_CONNECTION_LISTENER_CHANNELS_GETTER.invoke(MINECRAFT_SERVER.connection) as List<ChannelFuture>
        val serverChannel = channels.first().channel()
        val pipeline = serverChannel.pipeline()
        pipeline.addFirst("nova_pipeline_adapter", NovaServerChannelBootstrap)
    }
    
    @EventHandler
    private fun handleLogin(event: PlayerLoginEvent) {
        // find corresponding packet handler and set player instance
        val player = event.player
        val handler = MINECRAFT_SERVER.connection.connections
            .firstOrNull { (it.packetListener as? ServerLoginPacketListenerImpl)?.authenticatedProfile?.name == player.name }
            ?.channel
            ?.takeIf { it.isOpen }
            ?.let { it.pipeline().get(PACKET_HANDLER_NAME) as PacketHandler }
        if (handler != null) {
            handler.player = player
            handlers[player] = handler
        } else {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, Component.text("[Nova] Something went wrong"))
        }
    }
    
    @EventHandler
    private fun handleJoin(event: PlayerJoinEvent) {
        val handler = handlers[event.player]!!
        handler.loggedIn = true
    }
    
    @EventHandler
    private fun handleQuit(event: PlayerQuitEvent) {
        handlers -= event.player
    }
    
    private object NovaServerChannelBootstrap : ChannelInboundHandlerAdapter() {
        
        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            if (msg is Channel)
                msg.pipeline().addFirst(INIT_HANDLER_NAME, NovaChannelInitializer)
            super.channelRead(ctx, msg)
        }
        
    }
    
    @Sharable
    private object NovaChannelInitializer : ChannelInboundHandlerAdapter() {
        
        // uses ChannelInboundHandlerAdapter#channelActive instead of ChannelInitializer#initChannel
        // because Minecraft's handlers are not registered at that point
        
        override fun channelActive(ctx: ChannelHandlerContext) {
            ctx.fireChannelActive()
            ctx.pipeline().addBefore("packet_handler", PACKET_HANDLER_NAME, PacketHandler(ctx.channel()))
            ctx.pipeline().remove(this)
        }
        
    }
    
}