package xyz.xenondevs.nova.mixin.item.basedatacomponents;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.xenondevs.nova.world.item.NovaDataComponentMap;
import xyz.xenondevs.nova.world.item.NovaItem;

// Makes Holder.Reference bind components to Nova's NovaDataComponentMap
@Mixin(Holder.Reference.class)
abstract class HolderReferenceMixin<T> {
    
    @Shadow
    private @Nullable T value;
    
    @Shadow
    private @Nullable DataComponentMap components;
    
    @Inject(method = "bindComponents", at = @At("HEAD"), cancellable = true) 
    private void bindToNovaItemDataComponentMap(DataComponentMap components, CallbackInfo ci) {
        if (value instanceof NovaItem novaItem) {
            this.components = new NovaDataComponentMap(novaItem);
            ci.cancel();
        }
    }
    
}
