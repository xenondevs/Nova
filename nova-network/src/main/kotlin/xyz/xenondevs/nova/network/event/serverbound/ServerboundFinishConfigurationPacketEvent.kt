package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket
import xyz.xenondevs.nova.network.event.PacketEvent

class ServerboundFinishConfigurationPacketEvent(
    packet: ServerboundFinishConfigurationPacket
) : PacketEvent<ServerboundFinishConfigurationPacket>(packet)
