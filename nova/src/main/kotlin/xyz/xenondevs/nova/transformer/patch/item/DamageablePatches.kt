package xyz.xenondevs.nova.transformer.patch.item

import net.minecraft.stats.Stats
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.TypeInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.isClass
import xyz.xenondevs.bytebase.util.replaceEvery
import xyz.xenondevs.nova.item.behavior.Damageable
import xyz.xenondevs.nova.item.behavior.Wearable
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.VanillaToolCategory
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.bukkitMirror
import xyz.xenondevs.nova.util.item.DamageableUtils
import xyz.xenondevs.nova.util.item.ItemDamageResult
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.ITEM_STACK_HURT_AND_BREAK_METHOD
import java.util.function.Consumer

internal object DamageablePatches : MultiTransformer(ItemStack::class, Item::class, Inventory::class) {
    
    override fun transform() {
        transformItemStackHurtAndBreak()
        transformItemStackHurtEnemy()
        transformInventoryHurtArmor()
    }
    
    /**
     * Patches the ItemStack#hurtAndBreak method to properly damage Nova's tools.
     */
    private fun transformItemStackHurtAndBreak() {
        VirtualClassPath[ITEM_STACK_HURT_AND_BREAK_METHOD].instructions = buildInsnList {
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
        if (DamageableUtils.damageAndBreakItem(itemStack, damage, entity) == ItemDamageResult.BROKEN) {
            consumer.accept(entity)
        }
    }
    
    /**
     * Patches the ItemStack#hurtEnemy method to properly damage Nova's tools and with the proper damage values.
     */
    private fun transformItemStackHurtEnemy() {
        VirtualClassPath[ItemStack::hurtEnemy].instructions = buildInsnList {
                aLoad(0)
                aLoad(2)
                invokeStatic(::hurtEnemy)
                _return()
            }
    }
    
    @JvmStatic
    fun hurtEnemy(itemStack: ItemStack, player: Player) {
        val novaMaterial = itemStack.novaMaterial
        
        val damage = if (novaMaterial != null) {
            val damageable = novaMaterial.itemLogic.getBehavior(Damageable::class) ?: return
            damageable.options.itemDamageOnAttackEntity
        } else {
            val category = ToolCategory.ofItem(itemStack.bukkitMirror) as? VanillaToolCategory ?: return
            player.awardStat(Stats.ITEM_USED.get(itemStack.item))
            category.itemDamageOnAttackEntity
        }
        
        itemStack.hurtAndBreak(damage, player) {
            player.broadcastBreakEvent(EquipmentSlot.MAINHAND)
        }
    }
    
    /**
     * Patches the Inventory#hurtArmor method to recognize Nova's armor.
     */
    private fun transformInventoryHurtArmor() {
        VirtualClassPath[Inventory::hurtArmor].replaceEvery(1, 0, buildInsnList { 
            invokeStatic(::isArmorItem)
        }) { it.opcode == Opcodes.INSTANCEOF && (it as TypeInsnNode).isClass(ArmorItem::class) }
    }
    
    @JvmStatic
    fun isArmorItem(itemStack: ItemStack): Boolean {
        val novaMaterial = itemStack.novaMaterial ?: return itemStack.item is ArmorItem
        val novaItem = novaMaterial.itemLogic
        return novaItem.hasBehavior(Wearable::class) && novaItem.hasBehavior(Damageable::class)
    }
    
}