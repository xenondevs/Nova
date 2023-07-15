package xyz.xenondevs.nova.item.behavior

import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.map
import xyz.xenondevs.commons.provider.immutable.orElse
import xyz.xenondevs.commons.provider.immutable.provider
import xyz.xenondevs.nmsutils.network.PacketIdRegistry
import xyz.xenondevs.nmsutils.network.event.serverbound.ServerboundPlayerActionPacketEvent
import xyz.xenondevs.nmsutils.network.send
import xyz.xenondevs.nmsutils.util.removeIf
import xyz.xenondevs.nova.data.config.ConfigAccess
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.util.getPlayersNearby
import xyz.xenondevs.nova.util.intValue
import xyz.xenondevs.nova.util.isRightClick
import xyz.xenondevs.nova.util.item.genericMaxHealth
import xyz.xenondevs.nova.util.playSoundNearby
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.util.send
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.util.serverTick
import kotlin.math.min
import kotlin.random.Random

private const val PACKET_DISTANCE = 500.0

private data class Eater(val itemStack: ItemStack, val hand: EquipmentSlot, val startTime: Int)

fun FoodOptions(
    type: Consumable.FoodType,
    consumeTime: Int,
    nutrition: Int,
    saturationModifier: Float,
    instantHealth: Double = 0.0,
    effects: List<PotionEffect>? = null
) = Consumable.Default(
    provider(type),
    provider(consumeTime),
    provider(nutrition),
    provider(saturationModifier),
    provider(instantHealth),
    provider(effects)
)

/**
 * Allows items to be consumed.
 */
sealed interface Consumable { // TODO: remove sealed & make more customizable (make logic a patch?)
    
    /**
     * The type of food.
     */
    val type: FoodType
    
    /**
     * The time it takes for the food to be consumed, in ticks.
     */
    val consumeTime: Int
    
    /**
     * The nutrition value of the food.
     */
    val nutrition: Int
    
    /**
     * The saturation modifier this food provides. The saturation is calculated like this:
     * ```
     * saturation = min(saturation + nutrition * saturationModifier * 2.0f, foodLevel)
     * ```
     */
    val saturationModifier: Float
    
    /**
     * The amount of health to be restored immediately.
     */
    val instantHealth: Double
    
    /**
     * A list of effects to apply to the player when this food is consumed.
     */
    val effects: List<PotionEffect>?
    
    enum class FoodType(internal val property: VanillaMaterialProperty) {
        
        /**
         * Behaves like normal food.
         *
         * Has a small delay before the eating animation starts.
         *
         * Can only be eaten when hungry.
         */
        NORMAL(VanillaMaterialProperty.CONSUMABLE_NORMAL),
        
        /**
         * The eating animation starts immediately.
         *
         * Can only be eaten when hungry.
         */
        FAST(VanillaMaterialProperty.CONSUMABLE_FAST),
        
        /**
         * The food can always be eaten, no hunger is required.
         *
         * Has a small delay before the eating animation starts.
         */
        ALWAYS_EATABLE(VanillaMaterialProperty.CONSUMABLE_ALWAYS)
    }
    
    class Default(
        type: Provider<FoodType>,
        consumeTime: Provider<Int>,
        nutrition: Provider<Int>,
        saturationModifier: Provider<Float>,
        instantHealth: Provider<Double>,
        effects: Provider<List<PotionEffect>?>
    ) : ItemBehavior, Consumable {
        
        override val type by type
        override val consumeTime by consumeTime
        override val nutrition by nutrition
        override val saturationModifier by saturationModifier
        override val instantHealth by instantHealth
        override val effects by effects
        
        private val eaters = HashMap<Player, Eater>()
        
        init {
            runTaskTimer(0, 1) {
                eaters.removeIf { (player, eater) ->
                    if (serverTick >= eater.startTime + this.consumeTime) {
                        finishEating(player, eater)
                        return@removeIf true
                    } else handleEating(player, eater)
                    
                    return@removeIf false
                }
            }
        }
        
        override fun getVanillaMaterialProperties(): List<VanillaMaterialProperty> {
            return listOf(type.property)
        }
        
        override fun handleInteract(player: Player, itemStack: ItemStack, action: Action, event: PlayerInteractEvent) {
            // only right-clicks in the air or on non-interactive blocks will cause an eating process
            if (!action.isRightClick() || (action == Action.RIGHT_CLICK_BLOCK && event.clickedBlock!!.type.isInteractable))
                return
            
            // food which is not always consumable cannot be eaten in survival with a full hunger bar
            if (type != FoodType.ALWAYS_EATABLE && player.gameMode != GameMode.CREATIVE && event.player.foodLevel == 20)
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
            if (consumeTime < 20) {
                // if the consumeTime is smaller than 20 ticks, the initial wait of 7 ticks will be skipped
                playEatSound(player)
            } else {
                val eatTimePassed = serverTick - eater.startTime
                if ((type == FoodType.FAST || eatTimePassed > 7) && eatTimePassed % 4 == 0)
                    playEatSound(player)
            }
        }
        
        private fun playEatSound(player: Player) {
            player.location.playSoundNearby(Sound.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 1f, Random.nextDouble(0.8, 1.2).toFloat(), player)
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
            player.foodLevel = min(player.foodLevel + nutrition, 20)
            player.saturation = min(player.saturation + nutrition * saturationModifier * 2.0f, player.foodLevel.toFloat())
            player.health = min(player.health + instantHealth, player.genericMaxHealth)
            
            // effects
            effects?.forEach { player.addPotionEffect(it) }
            
            // sounds
            player.location.playSoundNearby(Sound.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.5f, Random.nextDouble(0.9, 1.0).toFloat())
            player.location.playSoundNearby(Sound.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 1.0f, Random.nextDouble(0.6, 1.4).toFloat())
            
            // take item
            if (player.gameMode != GameMode.CREATIVE)
                eater.itemStack.amount -= 1
        }
        
        private fun createEatingAnimationPacket(entityId: Int, active: Boolean, hand: EquipmentSlot): FriendlyByteBuf {
            val isOffHand = hand == EquipmentSlot.OFF_HAND
            
            val buf = FriendlyByteBuf(Unpooled.buffer())
            buf.writeVarInt(PacketIdRegistry.CLIENTBOUND_SET_ENTITY_DATA_PACKET)
            buf.writeVarInt(entityId) // entity id
            buf.writeByte(8) // type for eating animation
            buf.writeVarInt(0) // following is byte
            buf.writeByte(active.intValue or (isOffHand.intValue shl 1))
            buf.writeByte(0xff) // no more data
            
            return buf
        }
        
    }
    
    companion object : ItemBehaviorFactory<Default> {
        
        override fun create(item: NovaItem): Default {
            val cfg = ConfigAccess(item)
            return Default(
                cfg.getOptionalEntry<String>("food_type")
                    .map { FoodType.valueOf(it.uppercase()) }
                    .orElse(FoodType.NORMAL),
                cfg.getEntry<Int>("consume_time"),
                cfg.getEntry<Int>("nutrition"),
                cfg.getEntry<Float>("saturation_modifier"),
                cfg.getOptionalEntry<Double>("instant_health").orElse(0.0),
                cfg.getOptionalEntry<List<PotionEffect>>("effects")
            )
        }
        
    }
    
}