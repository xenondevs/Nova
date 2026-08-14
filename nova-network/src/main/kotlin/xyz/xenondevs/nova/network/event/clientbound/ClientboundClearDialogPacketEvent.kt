package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.common.ClientboundClearDialogPacket
import xyz.xenondevs.nova.network.event.PacketEvent

class ClientboundClearDialogPacketEvent(
    packet: ClientboundClearDialogPacket
) : PacketEvent<ClientboundClearDialogPacket>(packet)
