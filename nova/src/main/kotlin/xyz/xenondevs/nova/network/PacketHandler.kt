@file:Suppress("unused")

package xyz.xenondevs.nova.network

import io.netty.channel.Channel
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.PacketListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.network.event.PacketEventManager
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList

private typealias PacketCondition = (Packet<*>) -> Boolean

private interface PacketDropRequest {
    val condition: PacketCondition
}

private class IndefinitePacketDropRequest(override val condition: PacketCondition) : PacketDropRequest

private class LimitedPacketDropRequest(override val condition: PacketCondition, var n: Int) : PacketDropRequest

class PacketHandler internal constructor(val channel: Channel) : ChannelDuplexHandler() {
    
    private val queue = ConcurrentLinkedQueue<FriendlyByteBuf>()
    private val incomingDropQueue = CopyOnWriteArrayList<PacketDropRequest>()
    private val outgoingDropQueue = CopyOnWriteArrayList<PacketDropRequest>()
    
    @Volatile
    var player: Player? = null
        internal set
    
    override fun write(ctx: ChannelHandlerContext?, msg: Any?, promise: ChannelPromise?) {
        try {
            if (shouldDrop(msg, outgoingDropQueue))
                return
            
            if (msg is Packet<*>) {
                if (msg is ClientboundBundlePacket) {
                    val subPackets = msg.subPackets().mapNotNull(::callEvent)
                    if (subPackets.isEmpty())
                        return
                    super.write(ctx, ClientboundBundlePacket(subPackets), promise)
                } else {
                    val packet = callEvent(msg) ?: return
                    super.write(ctx, packet, promise)
                }
            } else {
                super.write(ctx, msg, promise)
            }
        } catch (t: Throwable) {
            LOGGER.error("An exception occurred while handling a clientbound packet.", t)
        }
    }
    
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        try {
            if (shouldDrop(msg, incomingDropQueue))
                return
            
            if (msg is Packet<*>) {
                val packet = callEvent(msg) ?: return
                super.channelRead(ctx, packet)
            } else {
                super.channelRead(ctx, msg)
            }
        } catch (t: Throwable) {
            LOGGER.error("An exception occurred while handling a serverbound packet.", t)
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
        } catch (t: Throwable) {
            LOGGER.error("An exception occurred trying to flush packets", t)
        }
    }
    
    private fun <L : PacketListener, P : Packet<in L>> callEvent(msg: P): P? {
        val event = PacketEventManager.createAndCallEvent(player, msg) ?: return msg
        return if (event.isCancelled) null else event.packet
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