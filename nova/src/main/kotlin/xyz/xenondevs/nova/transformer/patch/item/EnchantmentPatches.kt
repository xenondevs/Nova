@file:Suppress("IntroduceWhenSubject")

package xyz.xenondevs.nova.transformer.patch.item

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.EnchantmentMenu
import net.minecraft.world.inventory.GrindstoneMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.nova.item.behavior.Enchantable
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.ServerSoftware
import xyz.xenondevs.nova.util.ServerUtils
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.world.block.logic.tileentity.EnchantmentTableLogic
import xyz.xenondevs.nova.world.block.logic.tileentity.GrindstoneLogic

// for future reference: https://i.imgur.com/IGlChz6.png
private val ENCHANTMENT_MENU_SLOTS_CHANGED_LAMBDA = ReflectionUtils.getMethod(
    EnchantmentMenu::class,
    true,
    when {
        ServerSoftware.PAPER in ServerUtils.SERVER_SOFTWARE.tree -> "SRM(net.minecraft.world.inventory.EnchantmentMenu lambda\$slotsChanged\$0)"
        else -> "SRM(net.minecraft.world.inventory.EnchantmentMenu lambda\$0)"
    },
    ItemStack::class, Level::class, BlockPos::class
)

// for future reference: https://i.imgur.com/PyYEYD5.png
private val ENCHANTMENT_MENU_CLICK_MENU_BUTTON_LAMBDA = ReflectionUtils.getMethod(
    EnchantmentMenu::class,
    true, 
    when {
        ServerSoftware.PAPER in ServerUtils.SERVER_SOFTWARE.tree -> "SRM(net.minecraft.world.inventory.EnchantmentMenu lambda\$clickMenuButton\$1)"
        else -> "SRM(net.minecraft.world.inventory.EnchantmentMenu lambda\$1)"
    },
    ItemStack::class, Int::class, Player::class, Int::class, ItemStack::class, Level::class, BlockPos::class
)

private val GRINDSTONE_MENU_MERGE_ENCHANTS = ReflectionUtils.getMethod(
    GrindstoneMenu::class,
    true, "SRM(net.minecraft.world.inventory.GrindstoneMenu mergeEnchants)",
    ItemStack::class, ItemStack::class
)

private val GRINDSTONE_MENU_REMOVE_NON_CURSES = ReflectionUtils.getMethod(
    GrindstoneMenu::class,
    true, "SRM(net.minecraft.world.inventory.GrindstoneMenu removeNonCurses)",
    ItemStack::class, Int::class, Int::class
)

// for future reference: https://i.imgur.com/5R9nyxC.png
private val GRINDSTONE_MENU_RESULT_SLOT_CLASS = ReflectionUtils.getClass("SRC(net.minecraft.world.inventory.GrindstoneMenu$4)").kotlin
private val GRINDSTONE_MENU_RESULT_SLOT_GET_EXPERIENCE_FROM_ITEM = ReflectionUtils.getMethod(
    GRINDSTONE_MENU_RESULT_SLOT_CLASS,
    true,
    "SRM(net.minecraft.world.inventory.GrindstoneMenu\$4 getExperienceFromItem)",
    ItemStack::class
)

internal object EnchantmentPatches : MultiTransformer(ItemStack::class, EnchantmentMenu::class, GrindstoneMenu::class, GRINDSTONE_MENU_RESULT_SLOT_CLASS) {
    
    override fun transform() {
        VirtualClassPath[ItemStack::isEnchantable].delegateStatic(ReflectionUtils.getMethodByName(Enchantable::class, false, "isEnchantable"))
        VirtualClassPath[ItemStack::isEnchanted].delegateStatic(ReflectionUtils.getMethodByName(Enchantable::class, false, "isEnchanted"))
        VirtualClassPath[ENCHANTMENT_MENU_SLOTS_CHANGED_LAMBDA].delegateStatic(EnchantmentTableLogic::enchantmentMenuPrepareClues)
        VirtualClassPath[ENCHANTMENT_MENU_CLICK_MENU_BUTTON_LAMBDA].delegateStatic(EnchantmentTableLogic::enchantmentMenuEnchant)
        VirtualClassPath[GRINDSTONE_MENU_MERGE_ENCHANTS].delegateStatic(GrindstoneLogic::grindstoneMenuMergeEnchants)
        VirtualClassPath[GRINDSTONE_MENU_REMOVE_NON_CURSES].delegateStatic(GrindstoneLogic::grindstoneMenuRemoveNonCurses)
        VirtualClassPath[GRINDSTONE_MENU_RESULT_SLOT_GET_EXPERIENCE_FROM_ITEM].delegateStatic(GrindstoneLogic::resultSlotGetExperienceFromItem)
    }
    
}