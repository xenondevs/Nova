package xyz.xenondevs.nova.mixin.item.data;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.xenondevs.nova.world.item.legacy.ItemStackLegacyConversion;

@Mixin(ItemStack.class)
abstract class ItemStackMixin {
    
    @Shadow
    private Holder<Item> item;
    
    @Shadow
    private PatchedDataComponentMap components;
    
    @Inject(
        method = "<init>(Lnet/minecraft/core/Holder;ILnet/minecraft/core/component/DataComponentPatch;)V",
        at = @At("RETURN")
    )
    private void convertLegacyData(Holder<Item> item, int count, DataComponentPatch components, CallbackInfo ci) {
        var result = ItemStackLegacyConversion.convert(item, components);
        if (result == null)
            return;
        
        this.item = result.getItem();
        this.components = PatchedDataComponentMap.fromPatch(this.item.components(), result.getComponents());
    }
    
}
