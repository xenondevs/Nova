package xyz.xenondevs.nova.player.attachment

import com.mojang.datafixers.util.Pair
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.world.entity.EquipmentSlot
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.util.nmsCopy
import xyz.xenondevs.nova.util.send
import net.minecraft.world.item.ItemStack as MojangStack

/**
 * A special type of [ItemAttachment] that gets hidden for the player of they look down
 */
class HideDownItemAttachment(
    private val pitchThreshold: Float,
    player: Player,
    itemStack: ItemStack,
    position: Position = Position.BODY
) : ItemAttachment(player, itemStack, position) {
    
    private var hidden = false
    
    override fun handleTick() {
        super.handleTick()
        
        val pitch = player.location.pitch
        if (pitch >= pitchThreshold && !hidden) {
            // hide armor stand for attachment carrier
            val packet = ClientboundSetEquipmentPacket(armorStand.entityId, listOf(Pair(EquipmentSlot.HEAD, MojangStack.EMPTY)))
            player.send(packet)
            
            hidden = true
        } else if (hidden && pitch < pitchThreshold) {
            // show armor stand again
            val packet = ClientboundSetEquipmentPacket(armorStand.entityId, mutableListOf(Pair(EquipmentSlot.HEAD, itemStack.nmsCopy)))
            player.send(packet)
            
            hidden = false
        }
    }
    
}