@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.item.behavior

import io.papermc.paper.event.entity.EntityDamageItemEvent
import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import org.bukkit.GameMode
import org.bukkit.Statistic
import org.bukkit.craftbukkit.v1_20_R3.event.CraftEventFactory
import org.bukkit.craftbukkit.v1_20_R3.util.CraftMagicNumbers
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.mapNonNull
import xyz.xenondevs.commons.provider.immutable.orElse
import xyz.xenondevs.commons.provider.immutable.provider
import xyz.xenondevs.nova.data.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.data.serialization.json.serializer.RecipeDeserializer
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.logic.PacketItemData
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.util.bukkitMirror
import xyz.xenondevs.nova.util.callEvent
import xyz.xenondevs.nova.util.item.novaCompound
import xyz.xenondevs.nova.util.item.novaCompoundOrNull
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.nmsCopy
import xyz.xenondevs.nova.util.serverPlayer
import kotlin.random.Random
import net.minecraft.world.entity.LivingEntity as MojangLivingEntity
import net.minecraft.world.entity.player.Player as MojangPlayer
import net.minecraft.world.item.ItemStack as MojangStack
import org.bukkit.entity.LivingEntity as BukkitLivingEntity
import org.bukkit.entity.Player as BukkitPlayer
import org.bukkit.inventory.ItemStack as BukkitStack
import org.bukkit.inventory.meta.Damageable as BukkitDamageable

fun Damageable(
    maxDurability: Int,
    itemDamageOnAttackEntity: Int,
    itemDamageOnBreakBlock: Int,
    repairIngredient: RecipeChoice? = null
) = Damageable.Default(
    provider(maxDurability),
    provider(itemDamageOnAttackEntity),
    provider(itemDamageOnBreakBlock),
    provider(repairIngredient),
    true
)

/**
 * Allows items to store and receive damage.
 */
interface Damageable {
    
    val maxDurability: Int
    val itemDamageOnAttackEntity: Int
    val itemDamageOnBreakBlock: Int
    val repairIngredient: RecipeChoice?
    
    /**
     * Returns the current damage of the [itemStack].
     */
    fun getDamage(itemStack: BukkitStack): Int
    
    /**
     * Sets the damage of the [itemStack] to [damage].
     */
    fun setDamage(itemStack: BukkitStack, damage: Int)
    
    /**
     * Returns the current durability of the [itemStack].
     */
    fun getDurability(itemStack: BukkitStack): Int = maxDurability - getDamage(itemStack)
    
    /**
     * Sets the durability of the [itemStack] to [durability].
     */
    fun setDurability(itemStack: BukkitStack, durability: Int) = setDamage(itemStack, maxDurability - durability)
    
    /**
     * Adds [damage] to the current damage of the [itemStack] and returns whether the item broke.
     */
    fun damageAndBreak(itemStack: BukkitStack, damage: Int): Boolean
    
    /**
     * Returns the current damage of the [itemStack].
     */
    fun getDamage(itemStack: MojangStack): Int
    
    /**
     * Sets the damage of the [itemStack] to [damage].
     */
    fun setDamage(itemStack: MojangStack, damage: Int)
    
    /**
     * Returns the current durability of the [itemStack].
     */
    fun getDurability(itemStack: MojangStack): Int = maxDurability - getDamage(itemStack)
    
    /**
     * Sets the durability of the [itemStack] to [durability].
     */
    fun setDurability(itemStack: MojangStack, durability: Int) = setDamage(itemStack, maxDurability - durability)
    
    /**
     * Adds [damage] to the current damage of the [itemStack] and returns whether the item broke.
     */
    fun damageAndBreak(itemStack: MojangStack, damage: Int): Boolean
    
    class Default(
        maxDurability: Provider<Int>,
        damageOnAttackEntity: Provider<Int>,
        damageOnBreakBlock: Provider<Int>,
        repairIngredient: Provider<RecipeChoice?>,
        private val affectsItemDurability: Boolean
    ) : ItemBehavior, Damageable {
        
        override val maxDurability by maxDurability
        override val itemDamageOnAttackEntity by damageOnAttackEntity
        override val itemDamageOnBreakBlock by damageOnBreakBlock
        override val repairIngredient by repairIngredient
        
        override fun getDamage(itemStack: BukkitStack) = getDamage(itemStack.novaCompoundOrNull)
        override fun setDamage(itemStack: BukkitStack, damage: Int) = setDamage(itemStack.novaCompound, damage)
        override fun getDamage(itemStack: MojangStack) = getDamage(itemStack.novaCompoundOrNull)
        override fun setDamage(itemStack: MojangStack, damage: Int) = setDamage(itemStack.novaCompound, damage)
        
        override fun damageAndBreak(itemStack: BukkitStack, damage: Int): Boolean {
            val compound = itemStack.novaCompound
            val newDamage = getDamage(compound) + damage
            setDamage(compound, newDamage)
            return newDamage > maxDurability
        }
        
        override fun damageAndBreak(itemStack: MojangStack, damage: Int): Boolean {
            val compound = itemStack.novaCompound
            val newDamage = getDamage(compound) + damage
            setDamage(compound, newDamage)
            return newDamage >= maxDurability
        }
        
        private fun getDamage(compound: NamespacedCompound?): Int {
            return compound?.get<Int>("nova", "damage")?.coerceIn(0..maxDurability) ?: 0
        }
        
        private fun setDamage(compound: NamespacedCompound, damage: Int) {
            compound["nova", "damage"] = damage
        }
        
        private fun addDamage(compound: NamespacedCompound, damage: Int) {
            setDamage(compound, getDamage(compound) + damage)
        }
        
        override fun getVanillaMaterialProperties(): List<VanillaMaterialProperty> {
            return if (affectsItemDurability)
                listOf(VanillaMaterialProperty.DAMAGEABLE)
            else emptyList()
        }
        
        override fun getDefaultCompound(): NamespacedCompound {
            val compound = NamespacedCompound()
            compound["nova", "damage"] = 0
            return compound
        }
        
        override fun updatePacketItemData(data: NamespacedCompound, itemData: PacketItemData) {
            if (affectsItemDurability)
                itemData.durabilityBar = (maxDurability - getDamage(data)) / maxDurability.toDouble()
        }
        
    }
    
    companion object : ItemBehaviorFactory<Default> {
        
        override fun create(item: NovaItem): Default {
            val cfg = item.config
            return Default(
                cfg.entry<Int>(arrayOf("max_durability"), arrayOf("durability")),
                cfg.optionalEntry<Int>("item_damage_on_attack_entity").orElse(0),
                cfg.optionalEntry<Int>("item_damage_on_break_block").orElse(0),
                cfg.optionalEntry<Any>("repair_ingredient").mapNonNull {
                    val list = when (it) {
                        is String -> listOf(it)
                        else -> it as List<String>
                    }
                    RecipeDeserializer.parseRecipeChoice(list)
                },
                true
            )
        }
        
        // -- Bukkit ItemStack --
        
        /**
         * Damages the given [itemStack] while respecting the unbreaking enchantment.
         *
         * @param itemStack The [ItemStack] to damage
         * @param damage The amount of damage to add
         * @param breakCallback A callback that is called if the item breaks
         */
        fun damageAndBreak(itemStack: BukkitStack, damage: Int, breakCallback: (() -> Unit)? = null) =
            damageAndBreak(itemStack, damage, null, breakCallback?.let { { it() } })
        
        /**
         * Damages the given [itemStack] while respecting the unbreaking enchantment, calling events and criteria triggers and incrementing stats.
         *
         * @param itemStack The [ItemStack] to damage
         * @param damage The amount of damage to add
         * @param damager The entity that damaged the item, used for events, criteria triggers and stats
         * @param breakCallback A callback that is called if the item breaks
         */
        fun <T : BukkitLivingEntity?> damageAndBreak(itemStack: BukkitStack, damage: Int, damager: T, breakCallback: ((T) -> Unit)? = null) {
            // check for creative mode
            if (damager is BukkitPlayer && damager.gameMode == GameMode.CREATIVE)
                return
            // check if item is empty
            if (itemStack.isEmpty())
                return
            
            val item = itemStack.type
            val novaItem = itemStack.novaItem
            val damageable = novaItem?.getBehaviorOrNull<Damageable>()
            
            // check if the item is damageable
            if (novaItem != null && damageable == null || damageable == null && item.maxDurability <= 0 || itemStack.itemMeta?.isUnbreakable == true)
                return
            
            // build damage based on enchantments and events
            var actualDamage = damage
            val unbreakingLevel = itemStack.getEnchantmentLevel(Enchantment.DURABILITY)
            if (unbreakingLevel > 0)
                actualDamage = calculateActualDamage(actualDamage, unbreakingLevel, Wearable.isWearable(itemStack))
            
            if (damager is BukkitPlayer) {
                val event = PlayerItemDamageEvent(damager, itemStack, actualDamage, damage).also(::callEvent)
                
                if (actualDamage != event.damage || event.isCancelled)
                    damager.updateInventory()
                if (event.isCancelled)
                    return
            } else if (damager != null) {
                val event = EntityDamageItemEvent(damager, itemStack, actualDamage).also(::callEvent)
                if (event.isCancelled)
                    return
                actualDamage = event.damage
            }
            
            if (actualDamage <= 0)
                return
            
            // damage item
            val broken: Boolean
            if (damageable != null) {
                broken = damageable.damageAndBreak(itemStack, actualDamage)
            } else {
                val itemMeta = itemStack.itemMeta as BukkitDamageable
                val damageValue = itemMeta.damage + actualDamage
                broken = damageValue >= item.maxDurability
                if (damager is BukkitPlayer) {
                    CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(damager.serverPlayer, itemStack.nmsCopy, damageValue)
                    if (broken) damager.incrementStatistic(Statistic.BREAK_ITEM, item)
                }
                itemMeta.damage = damageValue
                itemStack.itemMeta = itemMeta
            }
            
            if (broken) {
                breakCallback?.invoke(damager)
                
                if (itemStack.amount == 1 && damager is BukkitPlayer)
                    callEvent(PlayerItemBreakEvent(damager, itemStack))
                
                itemStack.amount -= 1
                
                // reset damage value
                if (damageable != null) {
                    damageable.setDamage(itemStack, 0)
                } else {
                    val itemMeta = itemStack.itemMeta as BukkitDamageable
                    itemMeta.damage = 0
                    itemStack.itemMeta = itemMeta
                }
            }
        }
        
        /**
         * Checks whether this [itemStack] is of a damageable type and does not have the unbreakable tag.
         */
        fun isDamageable(itemStack: BukkitStack): Boolean {
            val novaItem = itemStack.novaItem
            if (novaItem != null)
                return novaItem.hasBehavior<Damageable>() && itemStack.itemMeta?.isUnbreakable != true
            
            return itemStack.type.maxDurability > 0 && itemStack.itemMeta?.isUnbreakable != true
        }
        
        /**
         * Gets the maximum durability of this [itemStack] or 0 if it is not of a damageable type.
         *
         * Note that a maximum durability larger than 0 does not necessarily mean that the item is damageable,
         * as the unbreakable tag is not checked.
         */
        fun getMaxDurability(itemStack: BukkitStack): Int {
            val novaItem = itemStack.novaItem
            if (novaItem != null)
                return novaItem.getBehaviorOrNull<Damageable>()?.maxDurability ?: 0
            
            return itemStack.type.maxDurability.toInt()
        }
        
        /**
         * Gets the current damage of this [itemStack] or 0 if it is not of a damageable type.
         */
        fun getDamage(itemStack: BukkitStack): Int {
            val novaItem = itemStack.novaItem
            if (novaItem != null)
                return novaItem.getBehaviorOrNull<Damageable>()?.getDamage(itemStack) ?: 0
            
            return (itemStack.itemMeta as? BukkitDamageable)?.damage ?: 0
        }
        
        /**
         * Sets the current damage of this [itemStack] if it is of a damageable type.
         */
        fun setDamage(itemStack: BukkitStack, damage: Int) {
            val novaItem = itemStack.novaItem
            if (novaItem != null) {
                novaItem.getBehaviorOrNull<Damageable>()?.setDamage(itemStack, damage)
            } else {
                val damageable = itemStack.itemMeta as? BukkitDamageable ?: return
                damageable.damage = damage
                itemStack.itemMeta = damageable
            }
        }
        
        /**
         * Checks whether [repairItem] is a valid repair ingredient for [item].
         */
        fun isValidRepairItem(item: BukkitStack, repairItem: BukkitStack): Boolean {
            val novaItem = item.novaItem
            if (novaItem != null)
                return novaItem.getBehaviorOrNull<Damageable>()?.repairIngredient?.test(repairItem) ?: false
            
            return CraftMagicNumbers.getItem(item.type).isValidRepairItem(item.nmsCopy, repairItem.nmsCopy)
        }
        
        // -- Mojang ItemStack --
        
        /**
         * Damages the given [itemStack] while respecting the unbreaking enchantment.
         *
         * @param itemStack The [ItemStack] to damage
         * @param damage The amount of damage to add
         * @param breakCallback A callback that is called if the item breaks
         */
        fun damageAndBreak(itemStack: MojangStack, damage: Int, breakCallback: (() -> Unit)? = null) =
            damageAndBreak(itemStack, damage, null, breakCallback?.let { { it() } })
        
        /**
         * Damages the given [itemStack] while respecting the unbreaking enchantment, calling events and criteria triggers and incrementing stats.
         *
         * @param itemStack The [ItemStack] to damage
         * @param damage The amount of damage to add
         * @param damager The entity that damaged the item, used for events, criteria triggers and stats
         * @param breakCallback A callback that is called if the item breaks
         */
        fun <T : MojangLivingEntity?> damageAndBreak(itemStack: MojangStack, damage: Int, damager: T, breakCallback: ((T) -> Unit)? = null) {
            // check for creative mode
            if (damager is MojangPlayer && damager.abilities.instabuild)
                return
            
            // check if item is empty
            if (itemStack.isEmpty)
                return
            
            val item = itemStack.item
            val novaItem = itemStack.novaItem
            val damageable = novaItem?.getBehaviorOrNull<Damageable>()
            
            // check if the item is damageable
            if (novaItem != null && damageable == null || damageable == null && item.maxDamage <= 0 || itemStack.tag?.getBoolean("Unbreakable") == true)
                return
            
            // build actual damage value based on enchantments and events
            var actualDamage = damage
            val unbreakingLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, itemStack)
            if (unbreakingLevel > 0)
                actualDamage = calculateActualDamage(actualDamage, unbreakingLevel, Wearable.isWearable(itemStack))
            
            if (damager is ServerPlayer) {
                val event = PlayerItemDamageEvent(damager.bukkitEntity, itemStack.bukkitMirror, actualDamage, damage).also(::callEvent)
                
                if (actualDamage != event.damage || event.isCancelled)
                    event.player.updateInventory()
                if (event.isCancelled)
                    return
                
                actualDamage = event.damage
            } else if (damager != null) {
                val event = EntityDamageItemEvent(damager.bukkitEntity, itemStack.bukkitMirror, actualDamage).also(::callEvent)
                if (event.isCancelled)
                    return
                actualDamage = event.damage
            }
            
            if (actualDamage <= 0)
                return
            
            // damage item
            val broken: Boolean
            if (damageable != null) {
                broken = damageable.damageAndBreak(itemStack, actualDamage)
            } else {
                val damageValue = itemStack.damageValue + actualDamage
                broken = damageValue >= itemStack.maxDamage
                if (damager is ServerPlayer) {
                    CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(damager, itemStack, damageValue)
                    if (broken) damager.awardStat(Stats.ITEM_BROKEN.get(item))
                }
                itemStack.damageValue = damageValue
            }
            
            if (broken) {
                breakCallback?.invoke(damager)
                
                if (itemStack.count == 1 && damager is MojangPlayer)
                    CraftEventFactory.callPlayerItemBreakEvent(damager, itemStack)
                
                itemStack.shrink(1)
                
                // reset damage value
                if (damageable != null) {
                    damageable.setDamage(itemStack, 0)
                } else {
                    itemStack.damageValue = 0
                }
            }
        }
        
        /**
         * Checks whether this [itemStack] is of a damageable type and does not have the unbreakable tag.
         */
        fun isDamageable(itemStack: MojangStack): Boolean {
            val novaItem = itemStack.novaItem
            if (novaItem != null)
                return novaItem.hasBehavior<Damageable>() && itemStack.tag?.getBoolean("Unbreakable") != true
            
            return itemStack.item.canBeDepleted()
        }
        
        /**
         * Gets the maximum durability of this [itemStack] or 0 if it is not of a damageable type.
         *
         * Note that a maximum durability larger than 0 does not necessarily mean that the item is damageable,
         * as the unbreakable tag is not checked.
         */
        fun getMaxDurability(itemStack: MojangStack): Int {
            val novaItem = itemStack.novaItem
            if (novaItem != null)
                return novaItem.getBehaviorOrNull<Damageable>()?.maxDurability ?: 0
            
            return itemStack.maxDamage
        }
        
        /**
         * Gets the current damage of this [itemStack] or 0 if it is not of a damageable type.
         */
        fun getDamage(itemStack: MojangStack): Int {
            val novaItem = itemStack.novaItem
            if (novaItem != null)
                return novaItem.getBehaviorOrNull<Damageable>()?.getDamage(itemStack) ?: 0
            
            return itemStack.damageValue
        }
        
        /**
         * Sets the current damage of this [itemStack] if it is of a damageable type.
         */
        fun setDamage(itemStack: MojangStack, damage: Int) {
            val novaItem = itemStack.novaItem
            if (novaItem != null) {
                novaItem.getBehaviorOrNull<Damageable>()?.setDamage(itemStack, damage)
            } else {
                itemStack.damageValue = damage
            }
        }
        
        /**
         * Checks whether [repairItem] is a valid repair ingredient for [item].
         */
        fun isValidRepairItem(item: MojangStack, repairItem: MojangStack): Boolean {
            val novaItem = item.novaItem
            if (novaItem != null)
                return novaItem.getBehaviorOrNull<Damageable>()?.repairIngredient?.test(repairItem.bukkitMirror) ?: false
            
            return item.item.isValidRepairItem(item, repairItem)
        }
        
        // -- Misc --
        
        private fun calculateActualDamage(damage: Int, unbreakingLevel: Int, isArmor: Boolean): Int {
            var actualDamage = 0
            repeat(damage) {
                if (isArmor) {
                    if (Random.nextFloat() > 0.6f)
                        actualDamage++
                } else {
                    if (Random.nextInt(unbreakingLevel + 1) == 0)
                        actualDamage++
                }
            }
            
            return actualDamage
        }
        
    }
    
}