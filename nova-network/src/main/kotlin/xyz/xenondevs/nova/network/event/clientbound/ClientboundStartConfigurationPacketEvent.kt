package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundStartConfigurationPacketEvent(
    player: Player,
    packet: ClientboundStartConfigurationPacket
) : PlayerPacketEvent<ClientboundStartConfigurationPacket>(player, packet)
