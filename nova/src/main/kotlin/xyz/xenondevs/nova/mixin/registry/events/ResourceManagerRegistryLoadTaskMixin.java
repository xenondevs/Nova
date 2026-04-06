package xyz.xenondevs.nova.mixin.registry.events;

import com.mojang.serialization.Lifecycle;
import io.papermc.paper.registry.data.util.Conversions;
import net.minecraft.resources.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.xenondevs.nova.registry.RegistryEventManager;

import java.util.Map;

@Mixin(ResourceManagerRegistryLoadTask.class)
abstract class ResourceManagerRegistryLoadTaskMixin<T> extends RegistryLoadTask<T> {
    
    @Unique
    public RegistryOps.RegistryInfoLookup nova$lookup = null;
    
    protected ResourceManagerRegistryLoadTaskMixin(
        RegistryDataLoader.RegistryData<T> data,
        Lifecycle lifecycle,
        Map<ResourceKey<?>, Exception> loadingErrors
    ) {
        super(data, lifecycle, loadingErrors);
    }
    
    @Override
    public boolean freezeRegistry(Map<ResourceKey<?>, Exception> loadingErrors) {
        return super.freezeRegistry(loadingErrors);
    }
    
    @Inject(
        method = "lambda$load$3",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/resources/ResourceManagerRegistryLoadTask;registerElements(Ljava/util/stream/Stream;Lio/papermc/paper/registry/data/util/Conversions;)V"
        )
    )
    private void preFreeze(
        Conversions conversions,
        Map<?, ?> loadedEntries,
        CallbackInfo ci
    ) {
        nova$lookup = conversions.lookup();
        RegistryEventManager.handlePreFreeze(registry, conversions.lookup());
    }
    
}
