@file:Suppress("JoinDeclarationAndAssignment")

package xyz.xenondevs.nova.world.player.attachment

import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.joml.Math
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector3fc
import xyz.xenondevs.commons.collections.mapToIntArray
import xyz.xenondevs.nova.network.packet.ClientboundSetPassengersPacket
import xyz.xenondevs.nova.network.send
import xyz.xenondevs.nova.packetentity.PacketItemDisplay
import xyz.xenondevs.nova.packetentity.packetItemDisplay

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
    protected var passenger: PacketItemDisplay
    
    private var world = player.world
    
    init {
        passenger = createPassenger()
        passengerId = passenger.id
        passenger.spawn()
    }
    
    private fun createPassenger(): PacketItemDisplay {
        return packetItemDisplay {
            location by player.location.apply { y += 1.75; yaw = 0f; pitch = 0f }
            sendMovementPackets = false
            metadata {
                itemStack by this@ItemAttachment.itemStack
            }
            onSpawn {
                // This packet will be modified in AbilityManager to include all attachment entities
                viewer.send(ClientboundSetPassengersPacket(viewer.entityId, viewer.passengers.mapToIntArray(Entity::getEntityId)))
            }
        }
    }
    
    final override fun despawn() {
        passenger.despawn()
    }
    
    override fun handleTick() {
        updatePassengerLocation(player)
    }
    
    private fun updatePassengerLocation(player: Player) {
        if (player.world != world) {
            handleTeleport()
            world = player.world
        } else {
            val playerLocation = player.location
            
            val rotation = Quaternionf().rotateY(-Math.toRadians(playerLocation.yaw))
            val translate = Vector3f(translation).rotate(rotation)
            
            passenger.metadata.apply {
                scale = Vector3f(this@ItemAttachment.scale)
                leftRotation = rotation
                translation = translate
            }
            
            passenger.location = playerLocation.apply { y += 1.75; yaw = 0f; pitch = 0f }
        }
    }
    
    override fun handleTeleport() {
        passenger.despawn()
        passenger = createPassenger()
        passengerId = passenger.id
        passenger.spawn()
    }
    
}