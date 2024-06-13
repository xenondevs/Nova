@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nmsutils.network.event.clientbound

import net.kyori.adventure.text.Component
import net.md_5.bungee.api.chat.BaseComponent
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.internal.util.ReflectionRegistry.CLIENTBOUND_SET_ACTION_BAR_TEXT_PACKET_ADVENTURE_TEXT_FIELD
import xyz.xenondevs.nmsutils.internal.util.ReflectionRegistry.CLIENTBOUND_SET_ACTION_BAR_TEXT_PACKET_COMPONENTS_FIELD
import xyz.xenondevs.nmsutils.internal.util.toAdventureComponent
import xyz.xenondevs.nmsutils.internal.util.toBaseComponentArray
import xyz.xenondevs.nmsutils.internal.util.toNmsComponent
import xyz.xenondevs.nmsutils.network.event.PlayerPacketEvent
import net.minecraft.network.chat.Component as MojangComponent

class ClientboundActionBarPacketEvent(
    player: Player,
    packet: ClientboundSetActionBarTextPacket
) : PlayerPacketEvent<ClientboundSetActionBarTextPacket>(player, packet) {
    
    var text: MojangComponent = (CLIENTBOUND_SET_ACTION_BAR_TEXT_PACKET_ADVENTURE_TEXT_FIELD?.get(packet) as? Component)?.toNmsComponent()
        ?: (CLIENTBOUND_SET_ACTION_BAR_TEXT_PACKET_COMPONENTS_FIELD?.get(packet) as? Array<out BaseComponent>)?.toNmsComponent()
        ?: packet.text 
        set(value) {
            field = value
            changed = true
        }
    
    var bungeeText: Array<out BaseComponent>
        get() = text.toBaseComponentArray()
        set(value) {
            text = value.toNmsComponent()
        }
    
    var adventureText: Component
        get() = text.toAdventureComponent()
        set(value) {
            text = value.toNmsComponent()
        }
    
    override fun buildChangedPacket(): ClientboundSetActionBarTextPacket {
        return ClientboundSetActionBarTextPacket(text)
    }
    
}