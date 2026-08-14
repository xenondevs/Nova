package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundChunkBatchStartPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundChunkBatchStartPacketEvent(
    player: Player,
    packet: ClientboundChunkBatchStartPacket
) : PlayerPacketEvent<ClientboundChunkBatchStartPacket>(player, packet)
