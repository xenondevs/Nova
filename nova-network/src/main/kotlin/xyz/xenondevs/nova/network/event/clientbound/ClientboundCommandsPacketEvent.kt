package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundCommandsPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundCommandsPacketEvent(
    player: Player,
    packet: ClientboundCommandsPacket
) : PlayerPacketEvent<ClientboundCommandsPacket>(player, packet)
