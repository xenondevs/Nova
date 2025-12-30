package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundClientEndTickPacketEvent(
    player: Player,
    packet: ServerboundClientTickEndPacket
) : PlayerPacketEvent<ServerboundClientTickEndPacket>(player, packet)