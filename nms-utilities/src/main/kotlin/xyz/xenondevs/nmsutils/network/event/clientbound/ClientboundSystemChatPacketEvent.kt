package xyz.xenondevs.nmsutils.network.event.clientbound

import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.internal.util.toAdventureComponent
import xyz.xenondevs.nmsutils.internal.util.toNmsComponent
import xyz.xenondevs.nmsutils.network.event.PlayerPacketEvent

class ClientboundSystemChatPacketEvent(
    player: Player,
    packet: ClientboundSystemChatPacket
) : PlayerPacketEvent<ClientboundSystemChatPacket>(player, packet) {
    
    var overlay = packet.overlay
        set(value) {
            field = value
            changed = true
        }
    
    var message: Component = packet.content().toAdventureComponent()
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundSystemChatPacket {
        return ClientboundSystemChatPacket(message.toNmsComponent(), overlay)
    }
    
}