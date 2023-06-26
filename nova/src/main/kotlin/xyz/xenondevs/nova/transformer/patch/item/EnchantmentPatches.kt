package xyz.xenondevs.nova.transformer.patch.item

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.EnchantmentMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.nova.item.behavior.Enchantable
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.world.block.logic.tileentity.EnchantmentTableLogic

internal object EnchantmentPatches : MultiTransformer(ItemStack::class, EnchantmentMenu::class) {
    
    override fun transform() {
        VirtualClassPath[ItemStack::isEnchantable].delegateStatic(Enchantable::isEnchantable)
        VirtualClassPath[ItemStack::isEnchanted].delegateStatic(Enchantable::isEnchanted)
        transformEnchantmentMenuSlotsChanged()
        transformEnchantmentMenuClickMenuButton()
        
        dumpAll()
    }
    
    /**
     * Delegates EnchantmentMenu.lambda$slotsChanged$0 to [EnchantmentTableLogic.enchantmentMenuPrepareClues].
     */
    private fun transformEnchantmentMenuSlotsChanged() {
        // for future reference: https://i.imgur.com/IGlChz6.png
        val method = ReflectionUtils.getMethod(
            EnchantmentMenu::class, 
            true, "SRM(net.minecraft.world.inventory.EnchantmentMenu lambda\$slotsChanged\$0)",
            ItemStack::class, Level::class, BlockPos::class
        )
        VirtualClassPath[method].delegateStatic(EnchantmentTableLogic::enchantmentMenuPrepareClues)
    }
    
    /**
     * Delegates EnchantmentMenu.lambda$clickMenuButton$1 to [EnchantmentTableLogic.enchantmentMenuEnchant].
     */
    private fun transformEnchantmentMenuClickMenuButton() {
        // for future reference: 
        val method = ReflectionUtils.getMethod(
            EnchantmentMenu::class,
            true, "SRM(net.minecraft.world.inventory.EnchantmentMenu lambda\$clickMenuButton\$1)",
            ItemStack::class, Int::class, Player::class, Int::class, ItemStack::class, Level::class, BlockPos::class
        )
        VirtualClassPath[method].delegateStatic(EnchantmentTableLogic::enchantmentMenuEnchant)
    }
    
}