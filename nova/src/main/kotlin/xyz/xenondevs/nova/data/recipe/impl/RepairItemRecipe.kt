package xyz.xenondevs.nova.data.recipe.impl

import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.level.Level
import xyz.xenondevs.nova.item.behavior.Damageable
import xyz.xenondevs.nova.util.item.DamageableUtils
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.nmsCopy
import kotlin.math.max
import kotlin.math.min
import net.minecraft.world.item.crafting.RepairItemRecipe as MojangRepairItemRecipe

internal class RepairItemRecipe(id: ResourceLocation) : MojangRepairItemRecipe(id, CraftingBookCategory.MISC) {
    
    override fun matches(container: CraftingContainer, level: Level): Boolean {
        var firstStack: ItemStack? = null
        var secondStackFound = false
        for (itemStack in container.contents) {
            if (itemStack.isEmpty)
                continue
            
            if (!secondStackFound && DamageableUtils.isDamageable(itemStack) && itemStack.count == 1) {
                if (firstStack == null) {
                    firstStack = itemStack
                } else if (isSameItem(firstStack, itemStack)) {
                    secondStackFound = true
                } else return false
            } else return false
        }
        
        return firstStack != null && secondStackFound
    }
    
    private fun isSameItem(a: ItemStack, b: ItemStack): Boolean {
        val novaMaterial = a.novaMaterial
        return (novaMaterial != null && novaMaterial == b.novaMaterial)
            || (b.novaMaterial == null && a.item == b.item)
    }
    
    override fun assemble(container: CraftingContainer, registryAccess: RegistryAccess): ItemStack {
        val items = container.contents.filterNot(ItemStack::isEmpty)
        require(items.size == 2) { "Item size is not 2" }
        val novaMaterial = items[0].novaMaterial
        if (novaMaterial != null) {
            val damageable = novaMaterial.novaItem.getBehavior(Damageable::class)!!
            val maxDurability = damageable.options.durability
            
            val itemStackA = items[0]
            val itemStackB = items[1]
            
            val durabilityA = maxDurability - damageable.getDamage(itemStackA)
            val durabilityB = maxDurability - damageable.getDamage(itemStackB)
            
            val resultItem = novaMaterial.createItemStack().apply {
                val resultingDamage = maxDurability - min(durabilityA + durabilityB, maxDurability)
                damageable.setDamage(this, resultingDamage)
            }.nmsCopy
            
            // keep curse enchantments
            val enchantsA = EnchantmentHelper.getEnchantments(itemStackA)
            val enchantsB = EnchantmentHelper.getEnchantments(itemStackB)
            BuiltInRegistries.ENCHANTMENT.asSequence()
                .filter(Enchantment::isCurse)
                .forEach {
                    val level = max(enchantsA.getOrDefault(it, 0), enchantsB.getOrDefault(it, 0))
                    if (level > 0) {
                        resultItem.enchant(it, level)
                    }
                }
            
            return resultItem
        }
        
        // use the super method to repair non-Nova items
        return super.assemble(container, registryAccess)
    }
    
}