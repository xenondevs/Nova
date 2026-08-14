package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundLowDiskSpaceWarningPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundLowDiskSpaceWarningPacketEvent(
    player: Player,
    packet: ClientboundLowDiskSpaceWarningPacket
) : PlayerPacketEvent<ClientboundLowDiskSpaceWarningPacket>(player, packet)
