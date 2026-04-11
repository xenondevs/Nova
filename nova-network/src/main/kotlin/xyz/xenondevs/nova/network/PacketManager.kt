package xyz.xenondevs.nova.network

import io.netty.channel.Channel
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import net.minecraft.network.Connection
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * Gets the [PacketHandler] for this player, or null if no handler is bound to the player.
 * The handler can be assumed to be non-null after the player has finished joining the server.
 */
val Player.packetHandler: PacketHandler?
    get() = (this as CraftPlayer).handle.packetHandler

/**
 * Gets the [PacketHandler] for this player, or null if no handler is bound to the player.
 * The handler can be assumed to be non-null after the player has finished joining the server.
 */
val ServerPlayer.packetHandler: PacketHandler?
    get() = connection.connection.channel.pipeline().get(PacketManager.packetHandlerName) as? PacketHandler

/**
 * Sends raw byte [bufs] that represent encoded clientbound packets to this player,
 * which are optionally [retained][FriendlyByteBuf.retain] based on [retain],
 * and optionally immediately flushed based on [flush].
 */
fun Player.send(vararg bufs: FriendlyByteBuf, retain: Boolean = true, flush: Boolean = true) {
    val packetHandler = packetHandler ?: return
    bufs.forEach {
        if (retain) it.retain()
        packetHandler.queueByteBuf(it)
    }
    
    if (flush) packetHandler.channel.flush()
}

/**
 * Installs the packet handler in the default channel pipeline.
 * This sets up the packet event system and must be called on [plugin][plugin] [enable][JavaPlugin.onEnable],
 * before any players have joined the server.
 * (Nova addons do not need to call this)
 * 
 * @throws IllegalStateException If the packet handler is already installed.
 */
fun installPacketHandler(plugin: JavaPlugin) {
    PacketManager.init(plugin.namespace())
}

internal object PacketManager  {
    
    private lateinit var initHandlerName: String
    lateinit var packetHandlerName: String
        private set
    
    fun init(namespace: String) {
        if (::initHandlerName.isInitialized)
            throw IllegalStateException("Already installed")
        
        initHandlerName = "${namespace}_init"
        packetHandlerName = "${namespace}_packet_handler"
        
        MINECRAFT_SERVER.connection.channels.first().channel().pipeline()
            .addFirst("${namespace}_pipeline_adapter", NovaServerChannelBootstrap)
    }
    
    fun handlePlayerCreated(player: ServerPlayer, connection: Connection) {
        // May be null if the player disconnects during protocol change,
        // but ServerConfigurationPacketListener#handleConfigurationFinished still continues. (#693)
        // In that case it is ok to just not bind the packet handler, as the player is already disconnecting anyway.
        val handler = connection.channel.pipeline().get(packetHandlerName) as? PacketHandler
            ?: return
        handler.player = player.bukkitEntity
    }
    
    private object NovaServerChannelBootstrap : ChannelInboundHandlerAdapter() {
        
        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            if (msg is Channel)
                msg.pipeline().addFirst(initHandlerName, NovaChannelInitializer)
            super.channelRead(ctx, msg)
        }
        
    }
    
    @Sharable
    private object NovaChannelInitializer : ChannelInboundHandlerAdapter() {
        
        // uses ChannelInboundHandlerAdapter#channelActive instead of ChannelInitializer#initChannel
        // because Minecraft's handlers are not registered at that point
        
        override fun channelActive(ctx: ChannelHandlerContext) {
            ctx.fireChannelActive()
            ctx.pipeline().addBefore("packet_handler", packetHandlerName, PacketHandler(ctx.channel()))
            ctx.pipeline().remove(this)
        }
        
    }
    
}