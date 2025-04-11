package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.Vec3
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.ServerboundInteractPacket
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import java.lang.invoke.MethodHandles

private val ACTION =
    Class.forName("net.minecraft.network.protocol.game.ServerboundInteractPacket\$Action")
private val INTERACTION_ACTION =
    Class.forName("net.minecraft.network.protocol.game.ServerboundInteractPacket\$InteractionAction")
private val INTERACTION_AT_LOCATION_ACTION =
    Class.forName("net.minecraft.network.protocol.game.ServerboundInteractPacket\$InteractionAtLocationAction")

private val SERVERBOUND_INTERACT_PACKET_LOOKUP = MethodHandles
    .privateLookupIn(ServerboundInteractPacket::class.java, MethodHandles.lookup())
private val INTERACTION_ACTION_LOOKUP = MethodHandles
    .privateLookupIn(INTERACTION_ACTION, MethodHandles.lookup())
private val INTERACTION_AT_LOCATION_ACTION_LOOKUP = MethodHandles
    .privateLookupIn(INTERACTION_AT_LOCATION_ACTION, MethodHandles.lookup())
private val SERVERBOUND_INTERACT_PACKET_ENTITY_ID_GETTER = SERVERBOUND_INTERACT_PACKET_LOOKUP
    .findGetter(ServerboundInteractPacket::class.java, "entityId", Int::class.java)
private val SERVERBOUND_INTERACT_PACKET_ACTION_GETTER = SERVERBOUND_INTERACT_PACKET_LOOKUP
    .findGetter(ServerboundInteractPacket::class.java, "action", ACTION)
private val INTERACTION_ACTION_HAND_GETTER = INTERACTION_ACTION_LOOKUP
    .findGetter(INTERACTION_ACTION, "hand", InteractionHand::class.java)
private val INTERACTION_AT_LOCATION_ACTION_HAND_GETTER = INTERACTION_AT_LOCATION_ACTION_LOOKUP
    .findGetter(INTERACTION_AT_LOCATION_ACTION, "hand", InteractionHand::class.java)
private val INTERACTION_AT_LOCATION_ACTION_LOCATION_GETTER = INTERACTION_AT_LOCATION_ACTION_LOOKUP
    .findGetter(INTERACTION_AT_LOCATION_ACTION, "location", Vec3::class.java)

class ServerboundInteractPacketEvent(
    player: Player,
    packet: ServerboundInteractPacket
) : PlayerPacketEvent<ServerboundInteractPacket>(player, packet) {
    
    var entityId = SERVERBOUND_INTERACT_PACKET_ENTITY_ID_GETTER.invoke(packet) as Int
        set(value) {
            field = value
            changed = true
        }
    
    var action = Action.of(SERVERBOUND_INTERACT_PACKET_ACTION_GETTER.invoke(packet))
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
        
        data object Attack : Action
        
        companion object {
            
            internal fun of(nmsAction: Any): Action = when (nmsAction.javaClass) {
                INTERACTION_ACTION -> Interact(
                    INTERACTION_ACTION_HAND_GETTER.invoke(nmsAction) as InteractionHand
                )
                
                INTERACTION_AT_LOCATION_ACTION -> InteractAtLocation(
                    INTERACTION_AT_LOCATION_ACTION_HAND_GETTER.invoke(nmsAction) as InteractionHand,
                    INTERACTION_AT_LOCATION_ACTION_LOCATION_GETTER.invoke(nmsAction) as Vec3
                )
                
                // ServerboundInteractPacket.ATTACK anonymous class
                else -> Attack
            }
            
        }
        
    }
    
}