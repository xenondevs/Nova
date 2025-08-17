package xyz.xenondevs.nova.mixin.registry.events;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.registry.RegistryEventManager;

@Mixin(BuiltInRegistries.class)
abstract class BuiltInRegistriesMixin {
    
    @Redirect(
        method = "freeze",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/Registry;freeze()Lnet/minecraft/core/Registry;"
        )
    )
    private static <T> Registry<T> freeze(Registry<T> registry) {
        var lookup = new RegistryOps.HolderLookupAdapter(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        RegistryEventManager.handlePreFreeze((WritableRegistry<T>) registry, lookup);
        var frozen = registry.freeze();
        RegistryEventManager.handlePostFreeze(frozen, lookup);
        return frozen;
    }
    
}
