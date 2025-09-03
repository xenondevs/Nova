package xyz.xenondevs.nova.mixin.registry.injectwrapperblock;

import net.minecraft.core.DefaultedMappedRegistry;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DefaultedMappedRegistry.class)
abstract class DefaultMappedRegistryMixin<T> {
    
    @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
    private void getValue(ResourceLocation key, CallbackInfoReturnable<T> cir) {
        if (!key.getNamespace().equals("minecraft"))
            cir.setReturnValue(null);
    }
    
}
