package xyz.xenondevs.nova.network

import io.netty.channel.Channel
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.login.ServerboundHelloPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.network.event.PacketEventManager
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.logging.Level

class PacketHandler(private val channel: Channel) : ChannelDuplexHandler() {
    
    val queue = ConcurrentLinkedQueue<FriendlyByteBuf>()
    var player: Player? = null
    
    constructor(channel: Channel, player: Player) : this(channel) {
        this.player = player
        PacketManager.playerHandlers[player.name] = this
    }
    
    override fun write(ctx: ChannelHandlerContext?, msg: Any?, promise: ChannelPromise?) {
        val packet = callEvent(msg) ?: return
        super.write(ctx, packet, promise)
    }
    
    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        if (msg is ServerboundHelloPacket) {
            PacketManager.playerHandlers[msg.name] = this
            super.channelRead(ctx, msg)
        } else {
            val packet = callEvent(msg) ?: return
            super.channelRead(ctx, packet)
        }
    }
    
    override fun flush(ctx: ChannelHandlerContext?) {
        try {
            if (player != null) {
                while (queue.isNotEmpty()) {
                    channel.write(queue.poll().duplicate())
                }
            }
            super.flush(ctx)
        } catch (e: Exception) {
            LOGGER.log(Level.SEVERE, "An exception occurred trying to flush packets", e)
        }
    }
    
    private fun callEvent(msg: Any?): Any? {
        if (msg is Packet<*>) {
            val event = PacketEventManager.createAndCallEvent(player, msg) ?: return msg
            return if (event.isCancelled) null else event.packet
        }
        
        return msg
    }
    
}