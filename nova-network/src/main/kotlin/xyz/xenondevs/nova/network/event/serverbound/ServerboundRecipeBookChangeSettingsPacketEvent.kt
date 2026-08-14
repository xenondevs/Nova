package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket
import net.minecraft.world.inventory.RecipeBookType
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundRecipeBookChangeSettingsPacketEvent(
    player: Player,
    packet: ServerboundRecipeBookChangeSettingsPacket
) : PlayerPacketEvent<ServerboundRecipeBookChangeSettingsPacket>(player, packet) {
    
    var bookType: RecipeBookType = packet.bookType
        set(value) {
            field = value
            changed = true
        }
    
    var open = packet.isOpen
        set(value) {
            field = value
            changed = true
        }
    
    var filtering = packet.isFiltering
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ServerboundRecipeBookChangeSettingsPacket(bookType, open, filtering)
}
