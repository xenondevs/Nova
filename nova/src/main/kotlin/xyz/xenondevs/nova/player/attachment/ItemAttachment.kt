@file:Suppress("JoinDeclarationAndAssignment")

package xyz.xenondevs.nova.player.attachment

import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.joml.Math
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector3fc
import xyz.xenondevs.commons.collections.mapToIntArray
import xyz.xenondevs.nmsutils.network.ClientboundSetPassengersPacket
import xyz.xenondevs.nova.util.runTaskLater
import xyz.xenondevs.nova.util.send
import xyz.xenondevs.nova.world.fakeentity.impl.FakeItemDisplay

/**
 * An item model that attaches to a [Player] by using an [ArmorStand] as a passenger.
 *
 * @param player Specifies the [Player] that carries this [ItemAttachment].
 * @param itemStack The [ItemStack] to be used for the [ItemAttachment].
 */
open class ItemAttachment(
    final override val player: Player,
    val itemStack: ItemStack,
    val translation: Vector3fc = Vector3f(0f, 0f, 0f),
    val scale: Vector3fc = Vector3f(1f, 1f, 1f),
) : Attachment {
    
    final override var passengerId: Int
    protected var passenger: FakeItemDisplay
    
    init {
        passenger = createPassenger()
        passengerId = passenger.entityId
        passenger.register()
    }
    
    private fun createPassenger(): FakeItemDisplay {
        val spawnLoc = player.location.apply { yaw = 0f; pitch = 0f }
        return FakeItemDisplay(spawnLoc, false) { entity, data ->
            entity.spawnHandler = {
                runTaskLater(1) {
                    // This packet will be modified in AbilityManager to include all attachment entities
                    it.send(ClientboundSetPassengersPacket(player.entityId, player.passengers.mapToIntArray(Entity::getEntityId)))
                }
            }
            
            data.itemStack = itemStack
        }
    }
    
    final override fun despawn() {
        passenger.remove()
    }
    
    override fun handleTick() {
        val playerLocation = player.location
        
        val rotation = Quaternionf().rotateY(-Math.toRadians(playerLocation.yaw))
        val translate = Vector3f(translation).rotate(rotation)
        
        passenger.updateEntityData(true) {
            scale = Vector3f(this@ItemAttachment.scale)
            leftRotation = rotation
            translation = translate
        }
        
        passenger.updateLocationSilently(playerLocation.apply { yaw = 0f; pitch = 0f })
    }
    
    override fun handleTeleport() {
        passenger.remove()
        passenger.updateLocationSilently(player.location.apply { yaw = 0f; pitch = 0f })
        passenger.register()
    }
    
}