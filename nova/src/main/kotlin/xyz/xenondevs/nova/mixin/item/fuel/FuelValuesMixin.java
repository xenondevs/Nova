package xyz.xenondevs.nova.mixin.item.fuel;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.FuelValues;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.world.item.NovaItem;
import xyz.xenondevs.nova.world.item.behavior.Fuel;

@Mixin(FuelValues.class)
abstract class FuelValuesMixin {
    
    @Inject(method = "isFuel", at = @At("HEAD"), cancellable = true)
    private void isFuel(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (itemStack.getItem() instanceof NovaItem novaItem)
            cir.setReturnValue(novaItem.hasBehavior(Fuel.class));
    }
    
    @Inject(method = "burnDuration", at = @At("HEAD"), cancellable = true)
    private void burnDuration(ItemStack itemStack, CallbackInfoReturnable<Integer> cir) {
        if (itemStack.getItem() instanceof NovaItem novaItem) {
            var fuelBehavior = novaItem.getBehaviorOrNull(Fuel.class);
            cir.setReturnValue(fuelBehavior != null ? fuelBehavior.getBurnTime() : 0);
        }
    }
    
}
