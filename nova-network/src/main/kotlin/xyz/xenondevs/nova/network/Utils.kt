package xyz.xenondevs.nova.network

import io.netty.buffer.Unpooled
import it.unimi.dsi.fastutil.objects.Object2IntMap
import net.minecraft.core.RegistryAccess
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.IdDispatchCodec
import net.minecraft.network.protocol.PacketType
import net.minecraft.network.protocol.game.GameProtocols
import net.minecraft.server.MinecraftServer

internal val MINECRAFT_SERVER = MinecraftServer.getServer()
internal val REGISTRY_ACCESS = MINECRAFT_SERVER.registryAccess()

private val CLIENTBOUND_PACKET_IDS: Object2IntMap<PacketType<*>> = run {
    @Suppress("UNCHECKED_CAST")
    val codec = GameProtocols.CLIENTBOUND_TEMPLATE.bind(
        RegistryFriendlyByteBuf.decorator(REGISTRY_ACCESS)
    ).codec() as IdDispatchCodec<*, *, PacketType<*>>
    codec.toId
}

private val SERVERBOUND_PACKET_IDS: Object2IntMap<PacketType<*>> = run {
    @Suppress("UNCHECKED_CAST")
    val codec = GameProtocols.SERVERBOUND_TEMPLATE.bind(
        RegistryFriendlyByteBuf.decorator(REGISTRY_ACCESS),
        GameProtocols.Context { false }
    ).codec() as IdDispatchCodec<*, *, PacketType<*>>
    codec.toId
}

/**
 * Creates a new empty [RegistryFriendlyByteBuf] using the default [RegistryAccess].
 */
fun RegistryFriendlyByteBuf(): RegistryFriendlyByteBuf =
    RegistryFriendlyByteBuf(Unpooled.buffer(), REGISTRY_ACCESS)

internal fun getPacketId(packetType: PacketType<*>): Int {
    val id = CLIENTBOUND_PACKET_IDS.getOrDefault(packetType as Any, -1)
    if (id != -1)
        return id
    return SERVERBOUND_PACKET_IDS.getInt(packetType)
}