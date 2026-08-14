package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket
import xyz.xenondevs.nova.network.event.PacketEvent

class ServerboundLoginAcknowledgedPacketEvent(
    packet: ServerboundLoginAcknowledgedPacket
) : PacketEvent<ServerboundLoginAcknowledgedPacket>(packet)
