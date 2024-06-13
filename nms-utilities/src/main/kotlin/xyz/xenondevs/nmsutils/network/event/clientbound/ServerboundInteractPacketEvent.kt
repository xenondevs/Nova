package xyz.xenondevs.nmsutils.network.event.clientbound

import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.Vec3
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.internal.util.ReflectionRegistry.SERVERBOUND_INTERACT_PACKET_ACTION_FIELD
import xyz.xenondevs.nmsutils.internal.util.ReflectionRegistry.SERVERBOUND_INTERACT_PACKET_ENTITY_ID_FIELD
import xyz.xenondevs.nmsutils.internal.util.ReflectionRegistry.SERVERBOUND_INTERACT_PACKET_INTERACTION_ACTION_CLASS
import xyz.xenondevs.nmsutils.internal.util.ReflectionRegistry.SERVERBOUND_INTERACT_PACKET_INTERACTION_ACTION_HAND_FIELD
import xyz.xenondevs.nmsutils.internal.util.ReflectionRegistry.SERVERBOUND_INTERACT_PACKET_INTERACTION_AT_LOCATION_ACTION_CLASS
import xyz.xenondevs.nmsutils.internal.util.ReflectionRegistry.SERVERBOUND_INTERACT_PACKET_INTERACTION_AT_LOCATION_ACTION_HAND_FIELD
import xyz.xenondevs.nmsutils.internal.util.ReflectionRegistry.SERVERBOUND_INTERACT_PACKET_INTERACTION_AT_LOCATION_ACTION_LOCATION_FIELD
import xyz.xenondevs.nmsutils.network.ServerboundInteractPacket
import xyz.xenondevs.nmsutils.network.event.PlayerPacketEvent

class ServerboundInteractPacketEvent(
    player: Player,
    packet: ServerboundInteractPacket
) : PlayerPacketEvent<ServerboundInteractPacket>(player, packet) {
    
    var entityId = SERVERBOUND_INTERACT_PACKET_ENTITY_ID_FIELD.get(packet) as Int
        set(value) {
            field = value
            changed = true
        }
    
    var action = Action.of(SERVERBOUND_INTERACT_PACKET_ACTION_FIELD.get(packet))
        set(value) {
            field = value
            changed = true
        }
    
    var isUsingSecondaryAction = packet.isUsingSecondaryAction
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ServerboundInteractPacket =
        ServerboundInteractPacket(entityId, action, isUsingSecondaryAction)
    
    sealed interface Action {
        
        data class Interact(val hand: InteractionHand) : Action
        
        data class InteractAtLocation(val hand: InteractionHand, val location: Vec3) : Action
        
        object Attack : Action
        
        companion object {
            
            internal fun of(nmsAction: Any): Action = when (nmsAction.javaClass) {
                SERVERBOUND_INTERACT_PACKET_INTERACTION_ACTION_CLASS -> Interact(
                    SERVERBOUND_INTERACT_PACKET_INTERACTION_ACTION_HAND_FIELD.get(nmsAction) as InteractionHand
                )
                
                SERVERBOUND_INTERACT_PACKET_INTERACTION_AT_LOCATION_ACTION_CLASS -> InteractAtLocation(
                    SERVERBOUND_INTERACT_PACKET_INTERACTION_AT_LOCATION_ACTION_HAND_FIELD.get(nmsAction) as InteractionHand,
                    SERVERBOUND_INTERACT_PACKET_INTERACTION_AT_LOCATION_ACTION_LOCATION_FIELD.get(nmsAction) as Vec3
                )
                
                // ServerboundInteractPacket.ATTACK anonymous class
                else -> Attack
            }
            
        }
        
    }
    
}