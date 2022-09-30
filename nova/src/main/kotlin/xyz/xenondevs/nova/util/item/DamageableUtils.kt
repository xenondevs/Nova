package xyz.xenondevs.nova.util.item

import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import xyz.xenondevs.nova.util.bukkitMirror
import xyz.xenondevs.nova.util.callEvent
import kotlin.random.Random
import net.minecraft.world.item.ItemStack as MojangStack
import xyz.xenondevs.nova.item.behavior.Damageable as NovaDamageable

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
        
        val novaDamageable = itemStack.novaMaterial?.novaItem?.getBehavior(NovaDamageable::class)
        if (novaDamageable != null) {
            val newDamage = novaDamageable.getDamage(itemStack) + damage
            novaDamageable.setDamage(itemStack, newDamage)
            if (newDamage >= novaDamageable.maxDurability)
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
        if (entity is Player && entity.abilities.instabuild)
            return ItemDamageResult.UNDAMAGED
        
        // check if the item is damageable
        val novaDamageable = itemStack.novaMaterial?.novaItem?.getBehavior(NovaDamageable::class)
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
            val bukkitStack = itemStack.bukkitMirror
            
            if (entity is ServerPlayer)
                CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(entity, itemStack, novaDamageable.getDamage(bukkitStack) + damage)
            
            val newDamage = novaDamageable.getDamage(bukkitStack) + damage
            novaDamageable.setDamage(bukkitStack, newDamage)
            if (newDamage >= novaDamageable.maxDurability)
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
        val novaDamageable = itemStack.novaMaterial?.novaItem?.getBehavior(NovaDamageable::class)
        if (novaDamageable != null)
            return true
        
        return itemStack.type.maxDurability > 0
    }
    
    internal fun isDamageable(itemStack: MojangStack): Boolean {
        val novaDamageable = itemStack.novaMaterial?.novaItem?.getBehavior(NovaDamageable::class)
        if (novaDamageable != null)
            return true
        
        return itemStack.item.canBeDepleted()
    }
    
}

internal enum class ItemDamageResult {
    UNDAMAGED,
    DAMAGED,
    BROKEN
}