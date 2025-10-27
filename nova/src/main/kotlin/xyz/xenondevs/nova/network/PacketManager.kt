package xyz.xenondevs.nova.network

import io.netty.channel.Channel
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import net.minecraft.network.Connection
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.util.MINECRAFT_SERVER
import xyz.xenondevs.nova.util.registerEvents
import java.util.*
import net.minecraft.world.entity.player.Player as MojangPlayer

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
        
        MINECRAFT_SERVER.connection.channels.first().channel().pipeline()
            .addFirst("nova_pipeline_adapter", NovaServerChannelBootstrap)
    }
    
    fun handlePlayerCreated(player: ServerPlayer, connection: Connection) {
        val handler = connection.channel.pipeline().get(PACKET_HANDLER_NAME) as? PacketHandler
            ?: return
        handler.player = player.bukkitEntity
        handlers[player.bukkitEntity] = handler
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