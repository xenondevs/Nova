package xyz.xenondevs.nmsutils.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.network.event.PlayerPacketEvent

class ClientboundUpdateRecipesPacketEvent(
    player: Player,
    packet: ClientboundUpdateRecipesPacket
) : PlayerPacketEvent<ClientboundUpdateRecipesPacket>(player, packet) {
    
    var recipes = packet.recipes
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundUpdateRecipesPacket {
        return ClientboundUpdateRecipesPacket(recipes)
    }
    
}