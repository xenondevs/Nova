package xyz.xenondevs.nova.mixin.item.repair;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.level.ItemLike;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.util.item.ItemUtilsKt;

@Mixin(RepairItemRecipe.class)
abstract class RepairItemRecipeMixin {
    
    @Inject(method = "canCombine", at = @At("HEAD"), cancellable = true)
    private static void canCombine(ItemStack first, ItemStack second, CallbackInfoReturnable<Boolean> cir) {
        var firstNovaItem = ItemUtilsKt.getNovaItem(first);
        var secondNovaItem = ItemUtilsKt.getNovaItem(second);
        if (firstNovaItem != null || secondNovaItem != null)
            cir.setReturnValue(firstNovaItem == secondNovaItem);
    }
    
    @Redirect(
        method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;",
        at = @At(
            value = "NEW",
            target = "net/minecraft/world/item/ItemStack"
        )
    )
    private ItemStack newItemStack(ItemLike item, @Local(ordinal = 0) ItemStack template) {
        var novaItem = ItemUtilsKt.getNovaItem(template);
        if (novaItem != null)
            return CraftItemStack.unwrap(novaItem.createItemStack(1));
        return new ItemStack(item);
    }
    
}
