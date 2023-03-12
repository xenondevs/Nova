@file:Suppress("JoinDeclarationAndAssignment")

package xyz.xenondevs.nova.player.attachment

import net.minecraft.core.Rotations
import net.minecraft.world.entity.EquipmentSlot
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.collections.mapToIntArray
import xyz.xenondevs.nmsutils.network.ClientboundRotateHeadPacket
import xyz.xenondevs.nmsutils.network.ClientboundSetPassengersPacket
import xyz.xenondevs.nova.util.runTaskLater
import xyz.xenondevs.nova.util.send
import xyz.xenondevs.nova.util.serverTick
import xyz.xenondevs.nova.world.fakeentity.FakeEntity
import xyz.xenondevs.nova.world.fakeentity.impl.FakeArmorStand
import xyz.xenondevs.nova.world.fakeentity.impl.FakeSlime

@RequiresOptIn
annotation class ExperimentalAttachmentAPI

/**
 * An item model that attaches to a [Player] by using an [ArmorStand] as a passenger.
 *
 * @param player Specifies the [Player] that carries this [ItemAttachment].
 * @param itemStack The [ItemStack] to be used for the [ItemAttachment].
 */
@OptIn(ExperimentalAttachmentAPI::class)
open class ItemAttachment(
    final override val player: Player,
    val itemStack: ItemStack,
    private val position: Position = Position.BODY
) : Attachment {
    
    final override val passengerId: Int
    protected val armorStand: FakeArmorStand
    private val passengers: List<FakeEntity<*>>
    
    init {
        val spawnLoc = player.location.apply { yaw = 0f; pitch = 0f }
        armorStand = FakeArmorStand(spawnLoc, false) { ast, data ->
            ast.setEquipment(EquipmentSlot.HEAD, itemStack, false)
            data.isInvisible = true
            if (position == Position.BODY)
                data.isMarker = true
        }
        
        val passenger = when (position) {
            Position.BODY -> armorStand
            Position.HEAD -> {
                FakeSlime(spawnLoc, false) { _, data ->
                    data.size = -4
                    data.isInvisible = true
                }
            }
        }
        
        passenger.spawnHandler = {
            runTaskLater(1) {
                // This packet will be modified in AbilityManager to include all attachment armor stands
                it.send(ClientboundSetPassengersPacket(player.entityId, player.passengers.mapToIntArray(Entity::getEntityId)))
                
                if (position == Position.HEAD) {
                    // This packet will not be modified
                    // Set the armor stand as a passenger of the slime
                    it.send(ClientboundSetPassengersPacket(passenger.entityId, intArrayOf(armorStand.entityId)))
                }
            }
        }
        
        passengers = if (armorStand == passenger)
            listOf(armorStand)
        else listOf(passenger, armorStand)
        
        passengerId = passenger.entityId
        passengers.forEach(FakeEntity<*>::register)
    }
    
    final override fun despawn() {
        passengers.forEach(FakeEntity<*>::remove)
    }
    
    override fun handleTick() {
        if (serverTick % 10 == 0) {
            // teleport the armor stand near the player because it's not actually a passenger
            val tpLoc = when (position) {
                Position.BODY -> player.location
                Position.HEAD -> player.location.apply { yaw = 0f; pitch = 0f }
            }
            passengers.forEach { it.teleport(tpLoc) }
        }
        
        when (position) {
            Position.BODY -> {
                if (serverTick % 3 == 0) {
                    val headRotPacket = ClientboundRotateHeadPacket(passengerId, player.location.yaw)
                    armorStand.viewers.forEach { it.send(headRotPacket) }
                }
            }
            
            Position.HEAD -> {
                armorStand.updateEntityData(true) {
                    headRotation = Rotations(player.location.pitch, player.location.yaw, 0f)
                }
            }
        }
    }
    
    enum class Position {
        BODY,
        
        @ExperimentalAttachmentAPI
        HEAD
    }
    
}