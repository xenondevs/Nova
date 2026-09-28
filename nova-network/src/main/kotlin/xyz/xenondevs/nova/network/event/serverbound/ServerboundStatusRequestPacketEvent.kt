package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket
import xyz.xenondevs.nova.network.event.PacketEvent

class ServerboundStatusRequestPacketEvent(
    packet: ServerboundStatusRequestPacket
) : PacketEvent<ServerboundStatusRequestPacket>(packet)
