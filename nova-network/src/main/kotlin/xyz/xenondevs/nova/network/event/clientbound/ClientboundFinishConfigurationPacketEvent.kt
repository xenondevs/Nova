package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket
import xyz.xenondevs.nova.network.event.PacketEvent

class ClientboundFinishConfigurationPacketEvent(
    packet: ClientboundFinishConfigurationPacket
) : PacketEvent<ClientboundFinishConfigurationPacket>(packet)
