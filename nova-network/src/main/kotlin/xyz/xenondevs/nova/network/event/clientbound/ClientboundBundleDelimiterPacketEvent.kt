package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundBundleDelimiterPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundBundleDelimiterPacketEvent(
    player: Player,
    packet: ClientboundBundleDelimiterPacket
) : PlayerPacketEvent<ClientboundBundleDelimiterPacket>(player, packet)
