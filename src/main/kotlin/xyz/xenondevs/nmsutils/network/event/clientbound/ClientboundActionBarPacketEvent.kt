package xyz.xenondevs.nmsutils.network.event.clientbound

import net.kyori.adventure.text.Component
import net.md_5.bungee.api.chat.BaseComponent
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.internal.MutableLazy
import xyz.xenondevs.nmsutils.internal.util.toAdventureComponent
import xyz.xenondevs.nmsutils.internal.util.toBaseComponentArray
import xyz.xenondevs.nmsutils.internal.util.toNmsComponent
import xyz.xenondevs.nmsutils.network.event.PlayerPacketEvent
import net.minecraft.network.chat.Component as MojangComponent

class ClientboundActionBarPacketEvent(
    player: Player,
    packet: ClientboundSetActionBarTextPacket
) : PlayerPacketEvent<ClientboundSetActionBarTextPacket>(player, packet) {
    
    var text: MojangComponent by MutableLazy({ changed = true }) {
        packet.text
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