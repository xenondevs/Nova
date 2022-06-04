package xyz.xenondevs.nova.player.attachment

import net.minecraft.world.entity.EquipmentSlot
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand

/**
 * A model that attaches to a [Player] by using an [ArmorStand] as a passenger.
 *
 * @param player Specifies the [Player] that carries this [Attachment].
 * @param itemStack The [ItemStack] to be used for the [Attachment].
 */
open class Attachment(
    val player: Player,
    val itemStack: ItemStack
) {
    
    private val armorStand = FakeArmorStand(player.location) { ast, data ->
        ast.setEquipment(EquipmentSlot.HEAD, itemStack, false)
        data.invisible = true
        data.marker = true
        
        ast.spawnHandler = {
            println("spawn, sending packet to $it")
            // This packet will be modified in AbilityManager to include all attachment armor stands
            runTaskLater(1) {
                it.send(ClientboundSetPassengersPacket(player.entityId, player.passengers.mapToIntArray(Entity::getEntityId)))
            }
        }
    }
    val entityId = armorStand.entityId
    
    fun despawn() {
        armorStand.remove()
    }
    
    open fun handleTick() {
        if (serverTick % 10 == 0) {
            // teleport the armor stand near the player because it's not actually a passenger
            armorStand.teleport(player.location)
        }
    }
    
}