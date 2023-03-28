package xyz.xenondevs.nova.transformer.patch.item

import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.enchantment.EnchantmentCategory
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.replaceFirst
import xyz.xenondevs.nova.item.behavior.Damageable
import xyz.xenondevs.nova.item.behavior.Enchantable
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.item.novaCompound
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import kotlin.math.min
import net.minecraft.world.item.Item as MojangItem
import net.minecraft.world.item.ItemStack as MojangStack

@Suppress("unused")
internal object EnchantmentPatches : MultiTransformer(EnchantmentHelper::class, MojangItem::class, ExperienceOrb::class) {
    
    override fun transform() {
        patchEnchantmentTableEnchanting()
        patchMending()
    }
    
    private fun patchEnchantmentTableEnchanting() {
        VirtualClassPath[EnchantmentHelper::getAvailableEnchantmentResults]
            .replaceFirst(1, 0, buildInsnList {
                aLoad(1)
                invokeStatic(::canEnchantItemWith)
            }) { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(EnchantmentCategory::canEnchant) }
        
        val enchantmentValueAccessingMethods = mapOf(
            VirtualClassPath[EnchantmentHelper::getEnchantmentCost] to 3,
            VirtualClassPath[EnchantmentHelper::selectEnchantment] to 1
        )
        
        enchantmentValueAccessingMethods.forEach { (method, localIdx) ->
            method.replaceFirst(1, 0, buildInsnList {
                aLoad(localIdx)
                invokeStatic(::getEnchantmentValue)
            }) { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(MojangItem::getEnchantmentValue) }
        }
    
        VirtualClassPath[MojangItem::isEnchantable]
            .instructions = buildInsnList {
            aLoad(1)
            aLoad(0)
            invokeStatic(::isEnchantable)
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
        VirtualClassPath[ReflectionRegistry.EXPERIENCE_ORB_REPAIR_PLAYER_ITEMS_METHOD]
            .instructions = buildInsnList {
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
            val novaMaterial = it.novaMaterial
            if (novaMaterial != null) {
                val damage = novaMaterial.novaItem.getBehavior(Damageable::class)?.getDamage(it.novaCompound)
                return@getRandomItemWith damage != null && damage > 0
            }
            
            return@getRandomItemWith it.isDamaged
        }?.value
        
        if (itemStack != null) {
            val damageable = itemStack.novaMaterial?.novaItem?.getBehavior(Damageable::class)
            
            val repair: Int
            if (damageable != null) {
                val novaCompound = itemStack.novaCompound
                val damageValue = damageable.getDamage(novaCompound)
                repair = min(orb.value * 2, damageValue)
                damageable.setDamage(novaCompound, damageValue - repair)
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