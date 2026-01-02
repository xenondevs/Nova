package xyz.xenondevs.nova.network

import io.netty.channel.Channel
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import net.minecraft.network.Connection
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import org.bukkit.entity.Player
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.util.MINECRAFT_SERVER
import xyz.xenondevs.nova.util.PlayerMapManager
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
internal object PacketManager {
    
    val handlers: MutableMap<Player, PacketHandler> = PlayerMapManager.create()
    
    @Suppress("UNCHECKED_CAST")
    @InitFun
    private fun init() {
        MINECRAFT_SERVER.connection.channels.first().channel().pipeline()
            .addFirst("nova_pipeline_adapter", NovaServerChannelBootstrap)
    }
    
    fun handlePlayerCreated(player: ServerPlayer, connection: Connection) {
        // May be null if the player disconnects during protocol change,
        // but ServerConfigurationPacketListener#handleConfigurationFinished still continues. (#693)
        // In that case it is ok to just not bind the packet handler, as the player is already disconnecting anyway.
        val handler = connection.channel.pipeline().get(PACKET_HANDLER_NAME) as? PacketHandler
            ?: return
        handler.player = player.bukkitEntity
        handlers[player.bukkitEntity] = handler
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