package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundTagQueryPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundTagQueryPacketEvent(
    player: Player,
    packet: ClientboundTagQueryPacket
) : PlayerPacketEvent<ClientboundTagQueryPacket>(player, packet) {
    
    var transactionId = packet.transactionId
        set(value) {
            field = value
            changed = true
        }
    
    var tag: CompoundTag? = packet.tag
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ClientboundTagQueryPacket(transactionId, tag)
}
