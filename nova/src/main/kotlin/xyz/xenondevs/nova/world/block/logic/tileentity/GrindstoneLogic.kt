package xyz.xenondevs.nova.world.block.logic.tileentity

import net.minecraft.world.inventory.AnvilMenu
import net.minecraft.world.inventory.GrindstoneMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Items
import xyz.xenondevs.commons.collections.takeUnlessEmpty
import xyz.xenondevs.nova.item.behavior.Enchantable
import net.minecraft.world.item.ItemStack as MojangStack

object GrindstoneLogic {
    
    // TODO: createResult() logic
    // The grindstone table can also be used to repair items, which is done in the createResult() method.
    // However, Paper has removed the return type from the CraftEventFactory#callPrepareGrindstoneEvent() method,
    // which is called in createResult().
    // If we'd want to implement this we'd have to call that via reflection to create bytecode compatible with Paper.
    
    /**
     * Creates a copy of the [target] stack with the [source] stack's enchantments merged into it.
     */
    @JvmStatic
    fun grindstoneMenuMergeEnchants(menu: GrindstoneMenu, target: MojangStack, source: MojangStack): MojangStack {
        val result = target.copy()
        for ((enchantment, level) in Enchantable.getEnchantments(source)) {
            Enchantable.addEnchantment(result, enchantment, level)
        }
        
        return result
    }
    
    @JvmStatic
    fun grindstoneMenuRemoveNonCurses(menu: GrindstoneMenu, target: MojangStack, damage: Int, amount: Int): MojangStack {
        val curses = (Enchantable.getEnchantments(target).takeUnlessEmpty() ?: Enchantable.getStoredEnchantments(target))
            .filterTo(HashMap()) { it.key.isCurse }
        
        if (target.item == Items.ENCHANTED_BOOK && curses.isEmpty()) {
            val result = MojangStack(Items.BOOK)
            if (target.hasCustomHoverName()) {
                result.hoverName = target.hoverName
            }
            return result
        }
        
        val result = target.copyWithCount(amount)
        
        Enchantable.removeAllEnchantments(result)
        Enchantable.setEnchantments(result, curses)
        
        // TODO: custom damageable items (see todo above)
        if (damage > 0) {
            result.damageValue = damage
        } else {
            result.removeTagKey("Damage")
        }
        
        var repairCost = 0
        repeat(curses.size) { repairCost = AnvilMenu.calculateIncreasedRepairCost(repairCost) }
        result.setRepairCost(repairCost)
        
        return result
    }
    
    @JvmStatic
    fun resultSlotGetExperienceFromItem(slot: Slot, itemStack: MojangStack): Int {
        var exp = 0
        for ((enchantment, level) in Enchantable.getEnchantments(itemStack)) {
            if (!enchantment.isCurse) {
                exp += enchantment.getTableLevelRequirement(level).first
            }
        }
        
        return exp
    }
    
}