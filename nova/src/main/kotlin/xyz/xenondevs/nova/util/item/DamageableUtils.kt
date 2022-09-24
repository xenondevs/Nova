package xyz.xenondevs.nova.util.item

import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import xyz.xenondevs.nova.util.bukkitMirror
import kotlin.random.Random

object DamageableUtils {
    
    /**
     * Damages the given [item] while taking the unbreaking enchantment and unbreakable property into account.
     *
     * This method works for both vanilla and Nova tools.
     *
     * @return The same [ItemStack] with the durability possibly reduced or null if the item was broken.
     */
    fun damageItem(item: ItemStack, damage: Int = 1): ItemStack? {
        val meta = item.itemMeta ?: return item
        
        if (meta.isUnbreakable)
            return item
        
        val unbreakingLevel = meta.getEnchantLevel(Enchantment.DURABILITY)
        if (unbreakingLevel > 0 && Random.nextInt(0, unbreakingLevel + 1) > 0)
            return item
        
        val novaDamageable = item.novaMaterial?.novaItem?.getBehavior(xyz.xenondevs.nova.item.behavior.Damageable::class)
        if (novaDamageable != null) {
            val newDamage = novaDamageable.getDamage(item) + damage
            novaDamageable.setDamage(item, newDamage)
            if (newDamage >= novaDamageable.maxDurability)
                return null
        } else if (meta is Damageable && item.type.maxDurability > 0) {
            meta.damage += damage
            if (meta.damage >= item.type.maxDurability)
                return null
            item.itemMeta = meta
        }
        
        return item
    }
    
    /**
     * Damages an [itemStack] with damage amount [damage] for [entity].
     *
     * This method takes the unbreaking enchantment into consideration and also calls the item_durability_changed
     * criteria trigger if [entity] is not null.
     *
     * @return If the item is now broken
     */
    internal fun damageAndBreakItem(itemStack: net.minecraft.world.item.ItemStack, damage: Int, entity: LivingEntity?): ItemDamageResult {
        if (entity is Player && entity.abilities.instabuild)
            return ItemDamageResult.UNDAMAGED
        
        val unbreakingLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, itemStack)
        if (unbreakingLevel > 0 && Random.nextInt(0, unbreakingLevel + 1) > 0)
            return ItemDamageResult.UNDAMAGED
        
        var broken = false
        
        val novaDamageable = itemStack.novaMaterial?.novaItem?.getBehavior(xyz.xenondevs.nova.item.behavior.Damageable::class)
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
            itemStack.shrink(1)
            if (entity is Player && novaDamageable != null) {
                entity.awardStat(Stats.ITEM_BROKEN.get(itemStack.item))
            }
        }
        
        return if (broken) ItemDamageResult.BROKEN else ItemDamageResult.DAMAGED
    }
    
}

internal enum class ItemDamageResult {
    UNDAMAGED,
    DAMAGED,
    BROKEN
}