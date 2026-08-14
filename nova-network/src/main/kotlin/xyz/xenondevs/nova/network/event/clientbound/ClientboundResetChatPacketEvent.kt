package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.configuration.ClientboundResetChatPacket
import xyz.xenondevs.nova.network.event.PacketEvent

class ClientboundResetChatPacketEvent(
    packet: ClientboundResetChatPacket
) : PacketEvent<ClientboundResetChatPacket>(packet)
