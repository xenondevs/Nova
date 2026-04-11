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
import org.slf4j.LoggerFactory
import xyz.xenondevs.nova.network.event.PacketEventManager
import java.util.concurrent.ConcurrentLinkedQueue

private val LOGGER = LoggerFactory.getLogger(PacketHandler::class.java)

/**
 * The packet channel handler that is responsible for firing packet events and intercepting packets.
 * Use [injectIncoming] or [injectOutgoing] to inject packets into the netty pipeline starting at this handler.
 */
class PacketHandler internal constructor(val channel: Channel) : ChannelDuplexHandler() {
    
    private val queue = ConcurrentLinkedQueue<FriendlyByteBuf>()
    
    /**
     * The player associated with this packet handler,
     * or null if no player object exists yet (e.g., during the login or configuration phase).
     */
    @Volatile
    var player: Player? = null
        internal set
    
    override fun write(ctx: ChannelHandlerContext?, msg: Any?, promise: ChannelPromise?) {
        try {
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
    
    /**
     * Queues a raw byte buffer representing an encoded outgoing packet
     * that will be flushed later this tick.
     */
    fun queueByteBuf(buf: FriendlyByteBuf) {
        queue += buf
    }
    
    /**
     * Injects an incoming packet into the netty pipeline starting at this handler.
     * 
     * This may be useful for cases where an incoming packet was previously canceled, asynchronously handled,
     * and now a modified version needs to be re-inserted into the pipeline without firing packet events.
     */
    fun injectIncoming(msg: Any) {
        if (channel.eventLoop().inEventLoop()) {
            super.channelRead(channel.pipeline().context(this), msg)
        } else channel.eventLoop().execute {
            super.channelRead(channel.pipeline().context(this), msg)
        }
    }
    
    /**
     * Injects an outgoing packet into the netty pipeline starting at this handler.
     * 
     * This may be useful for cases where an outgoing packet was previously canceled, asynchronously handled,
     * and now a modified version needs to be re-inserted into the pipeline without firing packet events.
     */
    fun injectOutgoing(msg: Any, promise: ChannelPromise?) {
        if (channel.eventLoop().inEventLoop()) {
            super.write(channel.pipeline().context(this), msg, promise)
        } else channel.eventLoop().execute {
            super.write(channel.pipeline().context(this), msg, promise)
        }
    }
    
}