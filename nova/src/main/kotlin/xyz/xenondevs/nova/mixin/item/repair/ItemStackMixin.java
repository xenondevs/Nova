package xyz.xenondevs.nova.mixin.item.repair;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.util.item.ItemUtilsKt;
import xyz.xenondevs.nova.world.item.behavior.Damageable;

@Mixin(ItemStack.class)
abstract class ItemStackMixin {
    
    @Inject(method = "isValidRepairItem", at = @At("HEAD"), cancellable = true)
    private void isValidRepairItem(ItemStack ingredient, CallbackInfoReturnable<Boolean> cir) {
        var itemStack = (ItemStack) (Object) this;
        var novaItem = ItemUtilsKt.getNovaItem(itemStack);
        if (novaItem == null)
            return;
        
        var damageable = novaItem.getBehavior(Damageable.class);
        if (damageable == null)
            return;
        
        var repairIngredient = damageable.getRepairIngredient();
        if (repairIngredient == null)
            return;
        
        if (repairIngredient.test(ingredient.asBukkitMirror()))
            cir.setReturnValue(true);
    }
    
}
