package xyz.xenondevs.nova.transformer.patch.item

import net.minecraft.stats.Stats
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.TypeInsnNode
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.isClass
import xyz.xenondevs.bytebase.util.replaceEvery
import xyz.xenondevs.nova.item.behavior.Damageable
import xyz.xenondevs.nova.item.behavior.Wearable
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.VanillaToolCategory
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.bukkitMirror
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.ITEM_STACK_HURT_AND_BREAK_METHOD
import java.util.function.Consumer
import kotlin.math.min

internal object DamageablePatches : MultiTransformer(ItemStack::class, Item::class, Inventory::class, ExperienceOrb::class) {
    
    override fun transform() {
        transformItemStackHurtAndBreak()
        transformItemStackHurtEnemy()
        transformInventoryHurtArmor()
        transformExperienceOrbRepairPlayerItems()
    }
    
    /**
     * Patches the ItemStack#hurtAndBreak method to properly damage Nova's tools.
     */
    private fun transformItemStackHurtAndBreak() {
        VirtualClassPath[ITEM_STACK_HURT_AND_BREAK_METHOD].replaceInstructions {
            aLoad(0)
            iLoad(1)
            aLoad(2)
            aLoad(3)
            invokeStatic(::hurtAndBreak)
            _return()
        }
    }
    
    @JvmStatic
    fun hurtAndBreak(itemStack: ItemStack, damage: Int, entity: LivingEntity, consumer: Consumer<LivingEntity>) {
        Damageable.damageAndBreak(itemStack, damage, entity, consumer::accept)
    }
    
    /**
     * Patches the ItemStack#hurtEnemy method to properly damage Nova's tools and with the proper damage values.
     */
    private fun transformItemStackHurtEnemy() {
        VirtualClassPath[ItemStack::hurtEnemy].replaceInstructions {
            aLoad(0)
            aLoad(2)
            invokeStatic(::hurtEnemy)
            _return()
        }
    }
    
    @JvmStatic
    fun hurtEnemy(itemStack: ItemStack, player: Player) {
        val novaItem = itemStack.novaItem
        
        val damage: Int
        if (novaItem != null) {
            val damageable = novaItem.getBehaviorOrNull(Damageable::class) ?: return
            damage = damageable.itemDamageOnAttackEntity
        } else {
            val category = ToolCategory.ofItem(itemStack.bukkitMirror) as? VanillaToolCategory ?: return
            player.awardStat(Stats.ITEM_USED.get(itemStack.item))
            damage = category.itemDamageOnAttackEntity
        }
        
        itemStack.hurtAndBreak(damage, player) {
            player.broadcastBreakEvent(EquipmentSlot.MAINHAND)
        }
    }
    
    /**
     * Patches the Inventory#hurtArmor method to recognize Nova's armor.
     */
    private fun transformInventoryHurtArmor() {
        VirtualClassPath[Inventory::hurtArmor].replaceEvery(1, 0, {
            invokeStatic(::isArmorItem)
        }) { it.opcode == Opcodes.INSTANCEOF && (it as TypeInsnNode).isClass(ArmorItem::class) }
    }
    
    @JvmStatic
    fun isArmorItem(itemStack: ItemStack): Boolean {
        val novaItem = itemStack.novaItem ?: return itemStack.item is ArmorItem
        return novaItem.hasBehavior(Wearable::class) && novaItem.hasBehavior(Damageable::class)
    }
    
    private fun transformExperienceOrbRepairPlayerItems() {
        VirtualClassPath[ReflectionRegistry.EXPERIENCE_ORB_REPAIR_PLAYER_ITEMS_METHOD].replaceInstructions {
            aLoad(0)
            aLoad(1)
            iLoad(2)
            invokeStatic(::repairPlayerItems)
            ireturn()
        }
    }
    
    @JvmStatic
    fun repairPlayerItems(orb: ExperienceOrb, player: Player, exp: Int): Int {
        val itemStack = EnchantmentHelper.getRandomItemWith(Enchantments.MENDING, player) {
            val novaItem = it.novaItem
            if (novaItem != null) {
                val damage = novaItem.getBehaviorOrNull(Damageable::class)?.getDamage(it)
                return@getRandomItemWith damage != null && damage > 0
            }
            
            return@getRandomItemWith it.isDamaged
        }?.value
        
        if (itemStack != null) {
            val damageable = itemStack.novaItem?.getBehaviorOrNull(Damageable::class)
            
            val repair: Int
            if (damageable != null) {
                val damageValue = damageable.getDamage(itemStack)
                repair = min(orb.value * 2, damageValue)
                damageable.setDamage(itemStack, damageValue - repair)
            } else {
                repair = min(orb.value * 2, itemStack.damageValue)
                itemStack.damageValue -= repair
            }
            
            val remainingExp = exp - (repair / 2)
            return if (remainingExp > 0) repairPlayerItems(orb, player, remainingExp) else 0
        }
        
        return exp
    }
    
}