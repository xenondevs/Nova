package xyz.xenondevs.nova.world.item.behavior

import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.Items
import net.minecraft.world.item.ProjectileWeaponItem
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.util.nmsEntity
import xyz.xenondevs.nova.util.nmsInteractionHand
import xyz.xenondevs.nova.util.unwrap

/**
 * Defines how [Bow] behaves.
 */
interface BowLogic {
    
    /**
     * Whether [entity] can currently draw the bow.
     */
    fun canDraw(entity: LivingEntity, bow: ItemStack): Boolean
    
    /**
     * Called every tick while
     */
    fun handleDrawTick(entity: LivingEntity, bow: ItemStack, tick: Int)
    
    /**
     * Shoots a projectile after [entity] drew the bow for [chargeTime] ticks.
     */
    fun shoot(entity: LivingEntity, hand: EquipmentSlot, bow: ItemStack, chargeTime: Int): ItemStack
    
    /**
     * The default bow logic that implements vanilla behavior.
     */
    object Vanilla : BowLogic {
        
        override fun canDraw(entity: LivingEntity, bow: ItemStack): Boolean {
            if (entity is Player && entity.gameMode == GameMode.CREATIVE)
                return true
            
            return entity is InventoryHolder
                && entity.inventory.any { it != null && Tag.ITEMS_ARROWS.isTagged(it.type) }
        }
        
        override fun handleDrawTick(entity: LivingEntity, bow: ItemStack, tick: Int) = Unit
        
        override fun shoot(entity: LivingEntity, hand: EquipmentSlot, bow: ItemStack, chargeTime: Int): ItemStack {
            val nmsEntity = entity.nmsEntity
            val projectile = nmsEntity.getProjectile(ItemStack.of(Material.BOW).unwrap())
            if (projectile.isEmpty)
                return bow
            
            val powerForTime = BowItem.getPowerForTime(chargeTime)
            if (powerForTime < .1)
                return bow
            
            val projectileItems = ProjectileWeaponItem.draw(bow.unwrap(), projectile, nmsEntity)
            if (projectileItems.isEmpty())
                return bow
            
            val level = nmsEntity.level() as ServerLevel
            
            (Items.BOW as BowItem).shoot(
                level,
                nmsEntity, 
                hand.nmsInteractionHand, 
                bow.unwrap(), 
                projectileItems,
                powerForTime * 3f,
                1f,
                powerForTime == 1f,
                null,
                powerForTime
            )
            
            level.playSound(
                null,
                entity.x, entity.y, entity.z,
                SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS,
                1f,
                1f / (level.random.nextFloat() * 0.4f + 1.2f) + powerForTime * 0.5f
            )
            return bow
        }
        
    }
    
}

private val CAN_USE_KEY = NamespacedKey(Nova, "can_use_bow")
private const val USE_DURATION: Int = 72000

/**
 * An item behavior that makes the item behave like a bow, with customizable [logic].
 *
 * Example item model definition:
 * ```kotlin
 * modelDefinition {
 *     model = condition(ConditionItemModelProperty.UsingItem) {
 *         onTrue = rangeDispatch(RangeDispatchItemModelProperty.UseDuration) {
 *             fallback = buildModel { getModel("minecraft:item/bow_pulling_0") }
 *             entry[0.65] = buildModel { getModel("minecraft:item/bow_pulling_1") }
 *             entry[0.9] = buildModel { getModel("minecraft:item/bow_pulling_2") }
 *             scale = 0.05 // = 1/20 (20 ticks draw time)
 *         }
 *         onFalse = buildModel { getModel("minecraft:item/bow") }
 *     }
 * }
 * ```
 */
class Bow(private val logic: BowLogic = BowLogic.Vanilla) : ItemBehavior {
    
    override fun modifyUseDuration(entity: LivingEntity, itemStack: ItemStack, duration: Int): Int {
        if (logic.canDraw(entity, itemStack.clone()))
            return USE_DURATION
        return 0
    }
    
    override fun handleUseTick(entity: LivingEntity, itemStack: ItemStack, hand: EquipmentSlot, remainingUseTicks: Int) {
        val tick = USE_DURATION - remainingUseTicks
        logic.handleDrawTick(entity, itemStack.clone(), tick)
    }
    
    override fun handleUseStopped(entity: LivingEntity, itemStack: ItemStack, hand: EquipmentSlot, remainingUseTicks: Int) {
        val result = logic.shoot(entity, hand, itemStack.clone(), USE_DURATION - remainingUseTicks)
        entity.equipment?.setItem(hand, result)
    }
    
    //<editor-fold desc="disabling client-side prediction if necessary">
    override fun handleEquipmentTick(player: Player, itemStack: ItemStack, slot: EquipmentSlot) {
        // vanilla logic is compatible with the client's predictions
        if (logic == BowLogic.Vanilla || !slot.isHand)
            return
        
        val currentCanUse = itemStack.persistentDataContainer.has(CAN_USE_KEY)
        val canUse = logic.canDraw(player, itemStack.clone())
        
        if (currentCanUse && !canUse) {
            itemStack.editPersistentDataContainer { pdc -> pdc.remove(CAN_USE_KEY) }
            player.inventory.setItem(slot, itemStack)
        } else if (!currentCanUse && canUse) {
            itemStack.editPersistentDataContainer { pdc -> pdc.set(CAN_USE_KEY, PersistentDataType.BOOLEAN, true) }
            player.inventory.setItem(slot, itemStack)
        }
    }
    
    override fun modifyClientSideItemType(player: Player?, server: ItemStack, client: Material): Material {
        if (logic == BowLogic.Vanilla || server.persistentDataContainer.has(CAN_USE_KEY))
            return Material.BOW
        return client
    }
    //</editor-fold>
    
}