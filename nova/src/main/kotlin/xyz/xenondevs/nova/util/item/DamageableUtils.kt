@file:Suppress("unused")

package xyz.xenondevs.nova.util.item

import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import org.bukkit.craftbukkit.v1_20_R1.util.CraftMagicNumbers
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import xyz.xenondevs.nova.util.bukkitMirror
import xyz.xenondevs.nova.util.callEvent
import xyz.xenondevs.nova.util.nmsCopy
import xyz.xenondevs.nova.util.serverPlayer
import kotlin.random.Random
import net.minecraft.world.entity.player.Player as MojangPlayer
import net.minecraft.world.item.ItemStack as MojangStack
import xyz.xenondevs.nova.item.behavior.Damageable as NovaDamageable

/**
 * Damages the tool in the [player's][MojangPlayer] main hand by [damage] amount.
 */
fun Player.damageItemInMainHand(damage: Int = 1) {
    val serverPlayer = serverPlayer
    if (DamageableUtils.damageAndBreakItem(serverPlayer.mainHandItem, damage, serverPlayer) == ItemDamageResult.BROKEN) {
        serverPlayer.broadcastBreakEvent(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
    }
}

/**
 * Damages the tool in the [player's][MojangPlayer] offhand by [damage] amount.
 */
fun Player.damageItemInOffHand(damage: Int = 1) {
    val serverPlayer = serverPlayer
    if (DamageableUtils.damageAndBreakItem(serverPlayer.offhandItem, damage, serverPlayer) == ItemDamageResult.BROKEN) {
        serverPlayer.broadcastBreakEvent(net.minecraft.world.entity.EquipmentSlot.OFFHAND)
    }
}

/**
 * Damages the tool in the specified [hand] by [damage] amount.
 */
fun Player.damageItemInHand(hand: EquipmentSlot, damage: Int = 1) {
    when (hand) {
        EquipmentSlot.HAND -> damageItemInMainHand(damage)
        EquipmentSlot.OFF_HAND -> damageItemInOffHand(damage)
        else -> throw IllegalArgumentException("Not a hand: $hand")
    }
}

object DamageableUtils {
    
    /**
     * Damages the given [itemStack] while taking the unbreaking enchantment and unbreakable property into account.
     *
     * This method works for both vanilla and Nova tools.
     *
     * @return The same [ItemStack] with the durability possibly reduced or null if the item was broken.
     */
    fun damageItem(itemStack: ItemStack, damage: Int = 1): ItemStack? {
        val meta = itemStack.itemMeta ?: return itemStack
        
        if (meta.isUnbreakable)
            return itemStack
        
        val unbreakingLevel = meta.getEnchantLevel(Enchantment.DURABILITY)
        if (unbreakingLevel > 0 && Random.nextInt(0, unbreakingLevel + 1) > 0)
            return itemStack
        
        val novaDamageable = itemStack.novaItem?.getBehavior(NovaDamageable::class)
        if (novaDamageable != null) {
            val newDamage = novaDamageable.getDamage(itemStack) + damage
            novaDamageable.setDamage(itemStack, newDamage)
            if (newDamage >= novaDamageable.options.durability)
                return null
        } else if (meta is Damageable && itemStack.type.maxDurability > 0) {
            meta.damage += damage
            if (meta.damage >= itemStack.type.maxDurability)
                return null
            itemStack.itemMeta = meta
        }
        
        return itemStack
    }
    
    /**
     * Damages an [itemStack] with damage amount [damage] for [entity].
     *
     * This method takes the unbreaking enchantment into consideration and also calls the item_durability_changed
     * criteria trigger if [entity] is not null.
     *
     * @return If the item is now broken
     */
    @Suppress("NAME_SHADOWING")
    internal fun damageAndBreakItem(itemStack: MojangStack, damage: Int, entity: LivingEntity?): ItemDamageResult {
        var damage = damage
        
        // check for creative mode
        if (entity is MojangPlayer && entity.abilities.instabuild)
            return ItemDamageResult.UNDAMAGED
        
        // check if the item is damageable
        val novaDamageable = itemStack.novaItem?.getBehavior(NovaDamageable::class)
        if (novaDamageable == null && !itemStack.isDamageableItem)
            return ItemDamageResult.UNDAMAGED
        
        // consider unbreaking level
        val unbreakingLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, itemStack)
        if (unbreakingLevel > 0 && Random.nextInt(0, unbreakingLevel + 1) > 0)
            return ItemDamageResult.UNDAMAGED
        
        // fire PlayerItemDamageEvent
        if (entity is ServerPlayer) {
            val event = PlayerItemDamageEvent(entity.bukkitEntity, itemStack.bukkitMirror, damage)
            callEvent(event)
            
            damage = event.damage
            if (damage <= 0 || event.isCancelled)
                return ItemDamageResult.UNDAMAGED
        }
        
        // damage item
        var broken = false
        if (novaDamageable != null) {
            val novaCompound = itemStack.novaCompound
            
            if (entity is ServerPlayer)
                CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(entity, itemStack, novaDamageable.getDamage(novaCompound) + damage)
            
            val newDamage = novaDamageable.getDamage(novaCompound) + damage
            novaDamageable.setDamage(novaCompound, newDamage)
            if (newDamage >= novaDamageable.options.durability)
                broken = true
        } else if (itemStack.isDamageableItem) {
            if (entity is ServerPlayer)
                CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(entity, itemStack, itemStack.damageValue + damage)
            
            itemStack.damageValue += damage
            if (itemStack.damageValue >= itemStack.maxDamage)
                broken = true
        } else return ItemDamageResult.UNDAMAGED
        
        if (broken) {
            if (entity is ServerPlayer) {
                // only award stats for non-nova items
                if (novaDamageable == null)
                    entity.awardStat(Stats.ITEM_BROKEN.get(itemStack.item))
                
                // fire PlayerBreakItemEvent
                callEvent(PlayerItemBreakEvent(entity.bukkitEntity, itemStack.bukkitMirror))
            }
            
            // remove item
            itemStack.shrink(1)
        }
        
        return if (broken) ItemDamageResult.BROKEN else ItemDamageResult.DAMAGED
    }
    
    fun isDamageable(itemStack: ItemStack): Boolean {
        val novaDamageable = itemStack.novaItem?.getBehavior(NovaDamageable::class)
        if (novaDamageable != null)
            return true
        
        return itemStack.type.maxDurability > 0
    }
    
    fun getMaxDurability(itemStack: ItemStack): Int {
        val damageable = itemStack.novaItem?.getBehavior(NovaDamageable::class)
        if (damageable != null) {
            return damageable.options.durability
        }
        
        return itemStack.type.maxDurability.toInt()
    }
    
    fun getDamage(itemStack: ItemStack): Int {
        val damageable = itemStack.novaItem?.getBehavior(NovaDamageable::class)
        if (damageable != null) {
            return damageable.getDamage(itemStack)
        }
        
        return (itemStack.itemMeta as? Damageable)?.damage ?: 0
    }
    
    fun setDamage(itemStack: ItemStack, damage: Int) {
        val damageable = itemStack.novaItem?.getBehavior(NovaDamageable::class)
        if (damageable != null) {
            damageable.setDamage(itemStack, damage)
        } else {
            val itemMeta = itemStack.itemMeta as? Damageable ?: return
            itemMeta.damage = damage
            itemStack.itemMeta = itemMeta
        }
    }
    
    fun isValidRepairItem(first: ItemStack, second: ItemStack): Boolean {
        val damageable = first.novaItem?.getBehavior(NovaDamageable::class)
        if (damageable != null) {
            return damageable.options.repairIngredient?.test(second) ?: false
        }
        
        return CraftMagicNumbers.getItem(first.type).isValidRepairItem(first.nmsCopy, second.nmsCopy)
    }
    
    internal fun isDamageable(itemStack: MojangStack): Boolean {
        val novaDamageable = itemStack.novaItem?.getBehavior(NovaDamageable::class)
        if (novaDamageable != null)
            return true
        
        return itemStack.item.canBeDepleted()
    }
    
    internal fun getMaxDurability(itemStack: MojangStack): Int {
        val damageable = itemStack.novaItem?.getBehavior(NovaDamageable::class)
        if (damageable != null) {
            return damageable.options.durability
        }
        
        return itemStack.maxDamage
    }
    
    internal fun getDamage(itemStack: MojangStack): Int {
        val damageable = itemStack.novaItem?.getBehavior(NovaDamageable::class)
        if (damageable != null) {
            return damageable.getDamage(itemStack)
        }
        
        return itemStack.damageValue
    }
    
    internal fun setDamage(itemStack: MojangStack, damage: Int) {
        val damageable = itemStack.novaItem?.getBehavior(NovaDamageable::class)
        if (damageable != null) {
            damageable.setDamage(itemStack, damage)
        } else {
            itemStack.damageValue = damage
        }
    }
    
    internal fun isValidRepairItem(first: MojangStack, second: MojangStack): Boolean {
        val damageable = first.novaItem?.getBehavior(NovaDamageable::class)
        if (damageable != null) {
            return damageable.options.repairIngredient?.test(second.bukkitMirror) ?: false
        }
        
        return first.item.isValidRepairItem(first, second)
    }
    
}

internal enum class ItemDamageResult {
    UNDAMAGED,
    DAMAGED,
    BROKEN
}