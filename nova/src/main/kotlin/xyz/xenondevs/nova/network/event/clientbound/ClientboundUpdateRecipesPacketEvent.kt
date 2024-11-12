package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundUpdateRecipesPacketEvent(
    player: Player,
    packet: ClientboundUpdateRecipesPacket
) : PlayerPacketEvent<ClientboundUpdateRecipesPacket>(player, packet) {
    
    var itemSets = packet.itemSets
        set(value) {
            field = value
            changed = true
        }
    
    var stonecutterRecipes = packet.stonecutterRecipes
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundUpdateRecipesPacket {
        return ClientboundUpdateRecipesPacket(itemSets, stonecutterRecipes)
    }
    
}