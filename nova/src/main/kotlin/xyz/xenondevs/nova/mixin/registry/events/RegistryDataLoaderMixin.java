package xyz.xenondevs.nova.mixin.registry.events;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.registry.RegistryEventManager;

import java.util.List;

@Mixin(RegistryDataLoader.class)
abstract class RegistryDataLoaderMixin {
    
    @Definition(id = "registryInfoLookup", local = @Local(type = net.minecraft.resources.RegistryOps.RegistryInfoLookup.class))
    @Expression("registryInfoLookup = ?")
    @Inject(
        method = "load(Lnet/minecraft/resources/RegistryDataLoader$LoadingFunction;Ljava/util/List;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;",
        at = @At(value = "MIXINEXTRAS:EXPRESSION", shift = At.Shift.AFTER)
    )
    private static void preLoad(
        RegistryDataLoader.LoadingFunction loadingFunction,
        List<HolderLookup.RegistryLookup<?>> registryLookups,
        List<RegistryDataLoader.RegistryData<?>> registryData,
        CallbackInfoReturnable<RegistryAccess.Frozen> cir,
        @Local RegistryOps.RegistryInfoLookup lookup,
        @Local(ordinal = 2) List<RegistryDataLoader.Loader<?>> loaders
    ) {
        for (var loader : loaders) {
            var registry = loader.registry();
            RegistryEventManager.handlePreFreeze(registry, lookup);
        }
    }
    
    @Inject(
        method = "load(Lnet/minecraft/resources/RegistryDataLoader$LoadingFunction;Ljava/util/List;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;",
        at = @At("TAIL")
    )
    private static void postLoad(
        RegistryDataLoader.LoadingFunction loadingFunction,
        List<HolderLookup.RegistryLookup<?>> registryLookups,
        List<RegistryDataLoader.RegistryData<?>> registryData,
        CallbackInfoReturnable<RegistryAccess.Frozen> cir,
        @Local RegistryOps.RegistryInfoLookup lookup,
        @Local(ordinal = 2) List<RegistryDataLoader.Loader<?>> loaders
    ) {
        for (var loader : loaders) {
            var registry = loader.registry();
            RegistryEventManager.handlePostFreeze(registry, lookup);
        }
    }
    
}
