package xyz.xenondevs.nova.world.item.behavior

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ChargedProjectiles
import io.papermc.paper.datacomponent.item.ChargedProjectiles.chargedProjectiles
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.CrossbowItem
import net.minecraft.world.item.Items
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.commons.collections.isNotNullOrEmpty
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.ItemUse
import xyz.xenondevs.nova.resources.builder.layout.item.ChargedType
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.item.setCustomModelDataStrings
import xyz.xenondevs.nova.util.nmsEntity
import xyz.xenondevs.nova.util.nmsInteractionHand
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.InteractionResult
import xyz.xenondevs.nova.world.item.ItemAction
import xyz.xenondevs.nova.world.item.buildDataComponentMapProvider
import java.util.*
import kotlin.jvm.optionals.getOrNull

/**
 * Defines how [Crossbow] behaves.
 */
interface CrossbowLogic {
    
    /**
     * Checks whether [entity] can start drawing [crossbow].
     */
    fun canDraw(entity: LivingEntity, crossbow: ItemStack): Boolean
    
    /**
     * Gets the amount of ticks it takes for [entity] to fully draw [crossbow].
     */
    fun getDrawTime(entity: LivingEntity, crossbow: ItemStack): Int
    
    /**
     * Called every tick while [entity] is drawing [crossbow], with [tick] counting up from 0.
     */
    fun handleDrawTick(entity: LivingEntity, crossbow: ItemStack, tick: Int)
    
    /**
     * Chooses which projectiles to load into [crossbow] for [entity], returning the loaded projectiles,
     * or `null` if no projectiles can be loaded.
     */
    fun chooseProjectile(entity: LivingEntity, crossbow: ItemStack): ChargedProjectiles?
    
    /**
     * Shoots the loaded projectiles from [crossbow] that is in [entity's][entity] [hand], returning the resulting crossbow item stack.
     */
    fun shoot(entity: LivingEntity, hand: EquipmentSlot, crossbow: ItemStack): ItemStack
    
    /**
     * The default crossbow logic that implements vanilla behavior.
     */
    object Vanilla : CrossbowLogic {
        
        private val crossbowItem: CrossbowItem by lazy { Items.CROSSBOW as CrossbowItem }
        
        override fun canDraw(entity: LivingEntity, crossbow: ItemStack): Boolean {
            return !entity.nmsEntity.getProjectile(createDummyCrossbow(crossbow).unwrap()).isEmpty
        }
        
        override fun getDrawTime(entity: LivingEntity, crossbow: ItemStack): Int {
            return CrossbowItem.getChargeDuration(crossbow.unwrap(), entity.nmsEntity)
        }
        
        override fun handleDrawTick(entity: LivingEntity, crossbow: ItemStack, tick: Int) {
            val chargingSounds = crossbowItem.getChargingSounds(crossbow.unwrap())
            val chargeTime = getDrawTime(entity, crossbow)
            
            if (tick == (chargeTime * 0.2).toInt())
                playSoundIfPresent(entity, chargingSounds.start)
            
            if (tick == (chargeTime * 0.5).toInt())
                playSoundIfPresent(entity, chargingSounds.mid)
        }
        
        override fun chooseProjectile(entity: LivingEntity, crossbow: ItemStack): ChargedProjectiles? {
            val event = EntityLoadCrossbowEvent(entity, crossbow.clone(), entity.activeItemHand)
            if (!event.callEvent())
                return null
            
            // use actual crossbow item type, otherwise charging logic won't be run,
            // copy over enchantments as those affect the loading logic
            val dummyCrossbow = createDummyCrossbow(entity.activeItem)
            
            if (CrossbowItem.tryLoadProjectiles(entity.nmsEntity, dummyCrossbow.unwrap(), event.shouldConsumeItem())) {
                val chargingSounds = crossbowItem.getChargingSounds(crossbow.unwrap())
                playSoundIfPresent(entity, chargingSounds.end)
            }
            
            return dummyCrossbow.getData(DataComponentTypes.CHARGED_PROJECTILES)
        }
        
        override fun shoot(entity: LivingEntity, hand: EquipmentSlot, crossbow: ItemStack): ItemStack {
            val projectile = crossbow.unwrap().get(DataComponents.CHARGED_PROJECTILES)
                ?: return crossbow
            
            crossbowItem.performShooting(
                entity.nmsEntity.level(),
                entity.nmsEntity,
                hand.nmsInteractionHand,
                crossbow.unwrap(),
                CrossbowItem.getShootingPower(projectile),
                1f,
                null
            )
            
            return crossbow
        }
        
        private fun playSoundIfPresent(entity: LivingEntity, optSound: Optional<Holder<SoundEvent>>) {
            val sound = optSound.getOrNull()
                ?: return
            entity.world.serverLevel.playSound(
                null,
                entity.x, entity.y, entity.z,
                sound.value(),
                SoundSource.PLAYERS,
                0.5f, 1f
            )
        }
        
        private fun createDummyCrossbow(novaCrossbow: ItemStack): ItemStack {
            val dummyCrossbow = ItemStack.of(Material.CROSSBOW)
            novaCrossbow.getData(DataComponentTypes.ENCHANTMENTS)
                ?.let { dummyCrossbow.setData(DataComponentTypes.ENCHANTMENTS, it) }
            return dummyCrossbow
        }
        
    }
    
}

private val CAN_USE_KEY = NamespacedKey(Nova, "can_use_crossbow")
private const val USE_DURATION = 72000

/**
 * Makes items behave like crossbows, with customizable [logic].
 *
 * Example item model definition:
 * ```kotlin
 * modelDefinition {
 *     model = select(SelectItemModelProperty.ChargedType) {
 *         case[ChargedType.ARROW] = buildModel { getModel("minecraft:item/crossbow_arrow") }
 *         case[ChargedType.ROCKET] = buildModel { getModel("minecraft:item/crossbow_firework") }
 *         fallback = condition(ConditionItemModelProperty.UsingItem) {
 *             onTrue = rangeDispatch(RangeDispatchItemModelProperty.CrossbowPull) {
 *                 fallback = buildModel { getModel("minecraft:item/crossbow_pulling_0") }
 *                 entry[0.58] = buildModel { getModel("minecraft:item/crossbow_pulling_1") }
 *                 entry[1.0] = buildModel { getModel("minecraft:item/crossbow_pulling_2") }
 *             }
 *             onFalse = buildModel { getModel("minecraft:item/crossbow") }
 *         }
 *     }
 * }
 * ```
 * 
 * In item model definitions, [ChargedType] can only differentiate between rockets and arrows,
 * where [ChargedType.ARROW] is used for any non-rocket projectile. However, [ChargedProjectiles] allows loading
 * any item type into the crossbow. To address this inconsistency, this behavior writes the item ids
 * of all charged projectiles into the item's client-side custom model data, starting at index [customModelDataOffset].
 * 
 * Example item model definition using custom model data (with no offset):
 * ```kotlin
 * modelDefinition {
 *     model = select(SelectItemModelProperty.CustomModelData) {
 *         case["minecraft:arrow"] = buildModel { getModel("minecraft:item/crossbow_arrow") }
 *         // ... all other arrow types
 *         case["minecraft:firework_rocket"] = buildModel { getModel("minecraft:item/crossbow_firework") }
 *
 *         fallback = condition(ConditionItemModelProperty.UsingItem) {
 *             onTrue = rangeDispatch(RangeDispatchItemModelProperty.CrossbowPull) {
 *                 fallback = buildModel { getModel("minecraft:item/crossbow_pulling_0") }
 *                 entry[0.58] = buildModel { getModel("minecraft:item/crossbow_pulling_1") }
 *                 entry[1.0] = buildModel { getModel("minecraft:item/crossbow_pulling_2") }
 *             }
 *             onFalse = buildModel { getModel("minecraft:item/crossbow") }
 *         }
 *     }
 * }
 * ```
 */
class Crossbow(
    private val logic: CrossbowLogic = CrossbowLogic.Vanilla,
    private val customModelDataOffset: Int = 0
) : ItemBehavior {
    
    override val baseDataComponents = buildDataComponentMapProvider {
        this[DataComponentTypes.CHARGED_PROJECTILES] = chargedProjectiles(emptyList())
    }
    
    override fun use(itemStack: ItemStack, ctx: Context<ItemUse>): InteractionResult {
        val entity = ctx[ItemUse.SOURCE_LIVING_ENTITY] ?: return InteractionResult.Pass
        val hand = ctx[ItemUse.HELD_HAND] ?: return InteractionResult.Pass
        
        val projectiles = itemStack.getData(DataComponentTypes.CHARGED_PROJECTILES)?.projectiles()
        if (projectiles.isNotNullOrEmpty()) {
            val result = logic.shoot(entity, hand, itemStack.clone())
            return InteractionResult.Success(action = ItemAction.ConvertStack(result))
        }
        
        return InteractionResult.Pass
    }
    
    override fun modifyUseDuration(entity: LivingEntity, itemStack: ItemStack, duration: Int): Int {
        if (!isCharged(itemStack) && logic.canDraw(entity, itemStack.clone()))
            return USE_DURATION // not draw time, otherwise player will shoot immediately
        return 0
    }
    
    override fun handleUseTick(entity: LivingEntity, itemStack: ItemStack, hand: EquipmentSlot, remainingUseTicks: Int) {
        val tick = USE_DURATION - remainingUseTicks
        logic.handleDrawTick(entity, itemStack.clone(), tick)
        
        if (tick >= logic.getDrawTime(entity, itemStack.clone()) && !isCharged(itemStack)) {
            val projectiles = logic.chooseProjectile(entity, itemStack.clone())
                ?: return
            
            val chargedCrossbow = itemStack.clone().apply {
                setData(DataComponentTypes.CHARGED_PROJECTILES, projectiles)
            }
            
            entity.equipment?.setItem(hand, chargedCrossbow)
        }
    }
    
    override fun modifyClientSideStack(player: Player?, server: ItemStack, client: ItemStack): ItemStack {
        val projectiles = server.getData(DataComponentTypes.CHARGED_PROJECTILES)?.projectiles()
        if (projectiles.isNotNullOrEmpty()) {
            client.setCustomModelDataStrings(customModelDataOffset, projectiles.map { ItemUtils.getId(it).toString() })
        }
        
        return client
    }
    
    //<editor-fold desc="disabling client-side use prediction if necessary">
    override fun handleEquipmentTick(player: Player, itemStack: ItemStack, slot: EquipmentSlot) {
        // vanilla logic is compatible with the client's predictions
        if (logic == CrossbowLogic.Vanilla || !slot.isHand)
            return
        
        val currentCanUse = itemStack.persistentDataContainer.has(CAN_USE_KEY)
        val canUse = isCharged(itemStack) || logic.canDraw(player, itemStack.clone())
        
        if (currentCanUse && !canUse) {
            itemStack.editPersistentDataContainer { pdc -> pdc.remove(CAN_USE_KEY) }
            player.inventory.setItem(slot, itemStack)
        } else if (!currentCanUse && canUse) {
            itemStack.editPersistentDataContainer { pdc -> pdc.set(CAN_USE_KEY, PersistentDataType.BOOLEAN, true) }
            player.inventory.setItem(slot, itemStack)
        }
    }
    
    override fun modifyClientSideItemType(player: Player?, server: ItemStack, client: Material): Material {
        if (logic == CrossbowLogic.Vanilla || server.persistentDataContainer.has(CAN_USE_KEY))
            return Material.CROSSBOW
        return client
    }
    //</editor-fold>
    
    private fun isCharged(crossbow: ItemStack): Boolean =
        crossbow.getData(DataComponentTypes.CHARGED_PROJECTILES)?.projectiles().isNotNullOrEmpty()
    
}