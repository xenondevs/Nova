package xyz.xenondevs.nmsutils.network.event.clientbound

import net.kyori.adventure.text.Component
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.internal.util.ReflectionRegistry.CLIENTBOUND_SYSTEM_CHAT_PACKET_ADVENTURE_CONTENT_FIELD
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
    
    var message: String? = packet.content ?: (CLIENTBOUND_SYSTEM_CHAT_PACKET_ADVENTURE_CONTENT_FIELD?.get(packet) as Component?)?.toJson()
        set(value) {
            field = value
            changed = true
        }
    
    var bungeeMessage: Array<out BaseComponent>
        get() = message?.toBaseComponentArray() ?: arrayOf(TextComponent(""))
        set(value) {
            message = value.toJson()
        }
    
    var adventureMessage: Component
        get() = message?.toAdventureComponent() ?: Component.empty()
        set(value) {
            message = value.toJson()
        }
    
    override fun buildChangedPacket(): ClientboundSystemChatPacket {
        return ClientboundSystemChatPacket(message?.toNmsComponent(), overlay)
    }
    
}