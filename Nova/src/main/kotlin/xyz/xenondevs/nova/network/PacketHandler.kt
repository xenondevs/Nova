package xyz.xenondevs.nova.network

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.login.ServerboundHelloPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PacketEventManager

class PacketHandler() : ChannelDuplexHandler() {
    
    constructor(player: Player) : this() {
        this.player = player
    }
    
    var player: Player? = null
    
    override fun write(ctx: ChannelHandlerContext?, msg: Any?, promise: ChannelPromise?) {
        val packet = callEvent(msg) ?: return
        super.write(ctx, packet, promise)
    }
    
    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        if (msg is ServerboundHelloPacket) {
            PacketListener.playerHandlers[msg.gameProfile.name] = this
            super.channelRead(ctx, msg)
        } else {
            val packet = callEvent(msg) ?: return
            super.channelRead(ctx, packet)
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