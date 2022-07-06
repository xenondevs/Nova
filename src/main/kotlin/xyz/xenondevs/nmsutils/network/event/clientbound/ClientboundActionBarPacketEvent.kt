package xyz.xenondevs.nmsutils.network.event.clientbound

import net.md_5.bungee.api.chat.BaseComponent
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.internal.MutableLazy
import xyz.xenondevs.nmsutils.internal.util.toBaseComponentArray
import xyz.xenondevs.nmsutils.internal.util.toComponent
import xyz.xenondevs.nmsutils.network.event.PlayerPacketEvent

class ClientboundActionBarPacketEvent(
    player: Player,
    packet: ClientboundSetActionBarTextPacket
) : PlayerPacketEvent<ClientboundSetActionBarTextPacket>(player, packet) {
    
    var text: Array<BaseComponent>? by MutableLazy({ changed = true }) {
        packet.text?.toBaseComponentArray()
    }
    
    override fun buildChangedPacket(): ClientboundSetActionBarTextPacket {
        return ClientboundSetActionBarTextPacket(text?.toComponent())
    }
    
}