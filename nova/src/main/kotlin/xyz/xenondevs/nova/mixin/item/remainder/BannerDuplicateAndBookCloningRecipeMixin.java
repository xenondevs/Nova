package xyz.xenondevs.nova.mixin.item.remainder;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.BannerDuplicateRecipe;
import net.minecraft.world.item.crafting.BookCloningRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.util.item.ItemUtilsKt;

@Mixin(value = {BannerDuplicateRecipe.class, BookCloningRecipe.class})
abstract class BannerDuplicateAndBookCloningRecipeMixin {
    
    @Redirect(
        method = "getRemainingItems",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/Item;getCraftingRemainder()Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private static ItemStack getCraftingRemainer(
        Item item,
        @Local(argsOnly = true) CraftingInput input,
        @Local int slot
    ) {
        var novaItem = ItemUtilsKt.getNovaItem(input.getItem(slot));
        return novaItem != null
            ? CraftItemStack.unwrap(novaItem.getCraftingRemainingItem())
            : item.getCraftingRemainder();
    }
    
}
