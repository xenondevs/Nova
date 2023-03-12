package xyz.xenondevs.nmsutils.network.event.clientbound

import net.kyori.adventure.text.Component
import net.md_5.bungee.api.chat.BaseComponent
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.internal.util.toAdventureComponent
import xyz.xenondevs.nmsutils.internal.util.toBaseComponentArray
import xyz.xenondevs.nmsutils.internal.util.toJson
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
    
    var message = packet.content
        set(value) {
            field = value
            changed = true
        }
    
    var bungeeMessage: Array<out BaseComponent>
        get() = message.toBaseComponentArray()
        set(value) {
            message = value.toJson()
        }
    
    var adventureMessage: Component
        get() = message.toAdventureComponent()
        set(value) {
            message = value.toJson()
        }
    
    override fun buildChangedPacket(): ClientboundSystemChatPacket {
        return ClientboundSystemChatPacket(message.toNmsComponent(), overlay)
    }
    
}