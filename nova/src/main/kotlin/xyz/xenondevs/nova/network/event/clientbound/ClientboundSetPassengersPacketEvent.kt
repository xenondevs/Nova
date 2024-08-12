package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.ClientboundSetPassengersPacket
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundSetPassengersPacketEvent(
    player: Player,
    packet: ClientboundSetPassengersPacket
) : PlayerPacketEvent<ClientboundSetPassengersPacket>(player, packet) {
    
    var vehicle = packet.vehicle
        set(value) {
            field = value
            changed = true
        }
    var passengers = packet.passengers
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundSetPassengersPacket {
        return ClientboundSetPassengersPacket(vehicle, passengers)
    }
    
}   