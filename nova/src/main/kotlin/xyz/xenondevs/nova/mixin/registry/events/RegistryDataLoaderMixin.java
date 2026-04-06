package xyz.xenondevs.nova.mixin.registry.events;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryDataLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.xenondevs.nova.registry.RegistryEventManager;

import java.util.concurrent.CompletableFuture;

@Mixin(RegistryDataLoader.class)
abstract class RegistryDataLoaderMixin {
    
    @ModifyReturnValue(
        method = "load(Lnet/minecraft/resources/RegistryDataLoader$LoaderFactory;Ljava/util/List;Ljava/util/List;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;",
        at = @At("RETURN")
    )
    private static CompletableFuture<RegistryAccess.Frozen> runFreezeHandlers(
        CompletableFuture<RegistryAccess.Frozen> original
    ) {
        return original.thenApply(frozen -> {
            RegistryEventManager.handlePostFreeze(frozen);
            return frozen;
        });
    }
    
}
