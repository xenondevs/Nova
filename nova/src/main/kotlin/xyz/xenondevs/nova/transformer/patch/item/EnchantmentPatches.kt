package xyz.xenondevs.nova.transformer.patch.item

import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.enchantment.EnchantmentCategory
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.bytebase.util.replaceFirst
import xyz.xenondevs.nova.item.behavior.Damageable
import xyz.xenondevs.nova.item.behavior.Enchantable
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.bukkitMirror
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import kotlin.math.min
import kotlin.reflect.jvm.javaMethod
import net.minecraft.world.item.Item as MojangItem
import net.minecraft.world.item.ItemStack as MojangStack

internal object EnchantmentPatches : MultiTransformer(setOf(EnchantmentHelper::class, MojangItem::class, ExperienceOrb::class), computeFrames = true) {
    
    override fun transform() {
        patchEnchantmentTableEnchanting()
        patchMending()
    }
    
    private fun patchEnchantmentTableEnchanting() {
        val enchantmentHelperWrapper = classWrappers[EnchantmentHelper::class.internalName]!!
        
        enchantmentHelperWrapper[ReflectionRegistry.ENCHANTMENT_HELPER_GET_AVAILABLE_ENCHANTMENT_RESULTS_METHOD]!!
            .replaceFirst(1, 0, buildInsnList {
                aLoad(1)
                invokeStatic(EnchantmentPatches::canEnchantItemWith.javaMethod!!)
            }) { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(ReflectionRegistry.ENCHANTMENT_CATEGORY_CAN_ENCHANT_METHOD) }
        
        val enchantmentValueAccessingMethods = mapOf(
            enchantmentHelperWrapper[ReflectionRegistry.ENCHANTMENT_HELPER_GET_ENCHANTMENT_COST_METHOD]!! to 3,
            enchantmentHelperWrapper[ReflectionRegistry.ENCHANTMENT_HELPER_SELECT_ENCHANTMENT_METHOD]!! to 1
        )
        
        enchantmentValueAccessingMethods.forEach { (method, localIdx) ->
            method.replaceFirst(1, 0, buildInsnList {
                aLoad(localIdx)
                invokeStatic(::getEnchantmentValue.javaMethod!!)
            }) { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(ReflectionRegistry.ITEM_GET_ENCHANTMENT_VALUE_METHOD) }
        }
    
        classWrappers[MojangItem::class.internalName]!![ReflectionRegistry.ITEM_IS_ENCHANTABLE_METHOD]!!
            .instructions = buildInsnList {
            aLoad(1)
            aLoad(0)
            invokeStatic(EnchantmentPatches::isEnchantable.javaMethod!!)
            ireturn()
        }
    }
    
    @JvmStatic
    fun canEnchantItemWith(category: EnchantmentCategory, itemStack: MojangStack): Boolean {
        val novaMaterial = itemStack.novaMaterial
        if (novaMaterial != null) {
            val categories = itemStack.novaMaterial?.novaItem?.getBehavior(Enchantable::class)?.options?.enchantmentCategories
            return categories != null && category in categories
        }
        
        return category.canEnchant(itemStack.item)
    }
    
    @JvmStatic
    fun getEnchantmentValue(itemStack: MojangStack): Int {
        val novaMaterial = itemStack.novaMaterial
        if (novaMaterial != null) {
            return novaMaterial.novaItem.getBehavior(Enchantable::class)?.options?.enchantmentValue ?: 0
        }
        
        return itemStack.item.enchantmentValue
    }
    
    @JvmStatic
    fun isEnchantable(itemStack: MojangStack, item: MojangItem): Boolean {
        val novaMaterial = itemStack.novaMaterial
        if (novaMaterial != null) {
            return novaMaterial.novaItem.hasBehavior(Enchantable::class)
        }
        
        return item.maxStackSize == 1 && item.canBeDepleted()
    }
    
    private fun patchMending() {
        classWrappers[ExperienceOrb::class.internalName]!![ReflectionRegistry.EXPERIENCE_ORB_REPAIR_PLAYER_ITEMS_METHOD]!!
            .instructions = buildInsnList {
            aLoad(0)
            aLoad(1)
            iLoad(2)
            invokeStatic(::repairPlayerItems.javaMethod!!)
            ireturn()
        }
    }
    
    @JvmStatic
    fun repairPlayerItems(orb: ExperienceOrb, player: Player, exp: Int): Int {
        val itemStack = EnchantmentHelper.getRandomItemWith(Enchantments.MENDING, player) {
            val novaMaterial = it.novaMaterial
            if (novaMaterial != null) {
                val damage = novaMaterial.novaItem.getBehavior(Damageable::class)?.getDamage(it.bukkitMirror)
                return@getRandomItemWith damage != null && damage > 0
            }
            
            return@getRandomItemWith it.isDamaged
        }?.value
        
        if (itemStack != null) {
            val damageable = itemStack.novaMaterial?.novaItem?.getBehavior(Damageable::class)
            
            val repair: Int
            if (damageable != null) {
                val bukkitMirror = itemStack.bukkitMirror
                val damageValue = damageable.getDamage(bukkitMirror)
                repair = min(orb.value * 2, damageValue)
                damageable.setDamage(bukkitMirror, damageValue - repair)
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