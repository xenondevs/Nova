package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundConfigurationAcknowledgedPacketEvent(
    player: Player,
    packet: ServerboundConfigurationAcknowledgedPacket
) : PlayerPacketEvent<ServerboundConfigurationAcknowledgedPacket>(player, packet)
