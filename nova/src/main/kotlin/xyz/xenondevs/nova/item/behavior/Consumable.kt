package xyz.xenondevs.nova.item.behavior

import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nmsutils.network.event.serverbound.ServerboundPlayerActionPacketEvent
import xyz.xenondevs.nmsutils.network.send
import xyz.xenondevs.nova.material.options.FoodOptions
import xyz.xenondevs.nova.material.options.FoodType
import xyz.xenondevs.nova.util.getPlayersNearby
import xyz.xenondevs.nova.util.intValue
import xyz.xenondevs.nova.util.isRightClick
import xyz.xenondevs.nova.util.item.genericMaxHealth
import xyz.xenondevs.nova.util.playSoundNearby
import xyz.xenondevs.nova.util.removeIf
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.util.send
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.util.serverTick
import kotlin.math.min
import kotlin.random.Random

private const val SOUND_DISTANCE = 18.0
private const val PACKET_DISTANCE = 500.0

data class Eater(val itemStack: ItemStack, val hand: EquipmentSlot, val startTime: Int)

class Consumable(private val options: FoodOptions) : ItemBehavior() {
    
    override val vanillaMaterialProperties = listOf(options.type.vanillaMaterialProperty)
    
    private val eaters = HashMap<Player, Eater>()
    
    init {
        runTaskTimer(0, 1) {
            eaters.removeIf { (player, eater) ->
                if (serverTick >= eater.startTime + options.consumeTime) {
                    finishEating(player, eater)
                    return@removeIf true
                } else handleEating(player, eater)
                
                return@removeIf false
            }
        }
    }
    
    override fun handleInteract(player: Player, itemStack: ItemStack, action: Action, event: PlayerInteractEvent) {
        // only right-clicks in the air or on non-interactive blocks will cause an eating process
        if (!action.isRightClick() || (action == Action.RIGHT_CLICK_BLOCK && event.clickedBlock!!.type.isInteractable))
            return
        
        // food which is not always consumable cannot be eaten in survival with a full hunger bar
        if (options.type != FoodType.ALWAYS_EATABLE && player.gameMode != GameMode.CREATIVE && event.player.foodLevel == 20)
            return
        
        event.isCancelled = true
        beginEating(player, itemStack, event.hand!!)
    }
    
    override fun handleRelease(player: Player, itemStack: ItemStack, event: ServerboundPlayerActionPacketEvent) {
        runTask {
            val eater = eaters.remove(player)
            if (eater != null)
                stopEatingAnimation(player, eater)
        }
    }
    
    private fun handleEating(player: Player, eater: Eater) {
        if (options.consumeTime < 20) {
            // if the consumeTime is smaller than 20 ticks, the initial wait of 7 ticks will be skipped
            playEatSound(player)
        } else {
            val eatTimePassed = serverTick - eater.startTime
            if ((options.type == FoodType.FAST || eatTimePassed > 7) && eatTimePassed % 4 == 0)
                playEatSound(player)
        }
    }
    
    private fun playEatSound(player: Player) {
        player.location.playSoundNearby(SOUND_DISTANCE, Sound.ENTITY_GENERIC_EAT, 1f, Random.nextDouble(0.8, 1.2).toFloat(), player)
    }
    
    private fun beginEating(player: Player, itemStack: ItemStack, hand: EquipmentSlot) {
        // add to eaters map
        eaters[player] = Eater(itemStack, hand, serverTick)
        
        // send packet to begin eating particles for players nearby
        val packet = createEatingAnimationPacket(player.entityId, true, hand)
        player.location.getPlayersNearby(PACKET_DISTANCE, player).forEach { it.send(packet) }
    }
    
    private fun stopEatingAnimation(player: Player, eater: Eater) {
        // send packet to stop eating animation to other players
        val stopEatingAnimationPacket = createEatingAnimationPacket(player.entityId, false, eater.hand)
        player.location.getPlayersNearby(PACKET_DISTANCE, player).forEach { it.send(stopEatingAnimationPacket) }
    }
    
    private fun finishEating(player: Player, eater: Eater) {
        if (!player.isOnline)
            return
        
        // stop eating animation for other players
        stopEatingAnimation(player, eater)
        
        // send packet to stop eating
        val stopEatingPacket = ClientboundEntityEventPacket(player.serverPlayer, 9)
        player.send(stopEatingPacket)
        
        // food level / saturation / health
        player.foodLevel = min(player.foodLevel + options.nutrition, 20)
        player.saturation = min(player.saturation + options.nutrition * options.saturationModifier * 2.0f, player.foodLevel.toFloat())
        player.health = min(player.health + options.instantHealth, player.genericMaxHealth)
        
        // effects and custom code
        options.effects?.forEach { player.addPotionEffect(it) }
        options.custom?.invoke(player)
        
        // sounds
        player.location.playSoundNearby(SOUND_DISTANCE, Sound.ENTITY_PLAYER_BURP, 0.5f, Random.nextDouble(0.9, 1.0).toFloat())
        player.location.playSoundNearby(SOUND_DISTANCE, Sound.ENTITY_GENERIC_EAT, 1.0f, Random.nextDouble(0.6, 1.4).toFloat())
        
        // take item
        if (player.gameMode != GameMode.CREATIVE)
            eater.itemStack.amount -= 1
    }
    
    private fun createEatingAnimationPacket(entityId: Int, active: Boolean, hand: EquipmentSlot): FriendlyByteBuf {
        val isOffHand = hand == EquipmentSlot.OFF_HAND
        
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeVarInt(0x4D) // ClientboundSetEntityDataPacket
        buf.writeVarInt(entityId) // entity id
        buf.writeByte(8) // type for eating animation
        buf.writeVarInt(0) // following is byte
        buf.writeByte(active.intValue or (isOffHand.intValue shl 1))
        buf.writeByte(0xff) // no more data
        
        return buf
    }
    
}