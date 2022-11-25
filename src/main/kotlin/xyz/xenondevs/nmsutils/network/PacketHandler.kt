@file:Suppress("unused")

package xyz.xenondevs.nmsutils.network

import io.netty.channel.Channel
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.login.ServerboundHelloPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.LOGGER
import xyz.xenondevs.nmsutils.network.event.PacketEventManager
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.logging.Level

private typealias PacketCondition = (Packet<*>) -> Boolean

private interface PacketDropRequest {
    val condition: PacketCondition
}

private class IndefinitePacketDropRequest(override val condition: PacketCondition) : PacketDropRequest

private class LimitedPacketDropRequest(override val condition: PacketCondition, var n: Int) : PacketDropRequest

class PacketHandler internal constructor(private val channel: Channel) : ChannelDuplexHandler() {
    
    private val queue = ConcurrentLinkedQueue<FriendlyByteBuf>()
    private val incomingDropQueue = CopyOnWriteArrayList<PacketDropRequest>()
    private val outgoingDropQueue = CopyOnWriteArrayList<PacketDropRequest>()
    var player: Player? = null
    
    constructor(channel: Channel, player: Player) : this(channel) {
        this.player = player
        PacketManager.playerHandlers[player.name] = this
    }
    
    override fun write(ctx: ChannelHandlerContext?, msg: Any?, promise: ChannelPromise?) {
        if (shouldDrop(msg, outgoingDropQueue))
            return
        
        val packet = callEvent(msg) ?: return
        super.write(ctx, packet, promise)
    }
    
    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        if (msg is ServerboundHelloPacket) {
            PacketManager.playerHandlers[msg.name] = this
            super.channelRead(ctx, msg)
        } else {
            if (shouldDrop(msg, incomingDropQueue))
                return
            
            val packet = callEvent(msg) ?: return
            super.channelRead(ctx, packet)
        }
    }
    
    private fun shouldDrop(msg: Any?, list: MutableList<PacketDropRequest>): Boolean {
        if (msg !is Packet<*> || list.isEmpty())
            return false
        
        val iterator = list.iterator()
        for (request in iterator) {
            if (request.condition.invoke(msg)) {
                if (request is LimitedPacketDropRequest) {
                    request.n--
                    if (request.n <= 0)
                        iterator.remove()
                }
                
                return true
            }
        }
        
        return false
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
    
    fun queueByteBuf(buf: FriendlyByteBuf) {
        queue += buf
    }
    
    fun dropNextIncoming(n: Int, condition: PacketCondition) {
        incomingDropQueue += LimitedPacketDropRequest(condition, n)
    }
    
    fun dropAllIncoming(condition: PacketCondition) {
        incomingDropQueue += IndefinitePacketDropRequest(condition)
    }
    
    fun dropNextOutgoing(n: Int, condition: PacketCondition) {
        outgoingDropQueue += LimitedPacketDropRequest(condition, n)
    }
    
    fun dropAllOutgoing(condition: PacketCondition) {
        outgoingDropQueue += IndefinitePacketDropRequest(condition)
    }
    
    fun injectIncoming(msg: Any) {
        if (channel.eventLoop().inEventLoop()) {
            super.channelRead(channel.pipeline().context(this), msg)
        } else channel.eventLoop().execute {
            super.channelRead(channel.pipeline().context(this), msg)
        }
    }
    
    fun injectOutgoing(msg: Any, promise: ChannelPromise?) {
        if (channel.eventLoop().inEventLoop()) {
            super.write(channel.pipeline().context(this), msg, promise)
        } else channel.eventLoop().execute {
            super.write(channel.pipeline().context(this), msg, promise)
        }
    }
    
}