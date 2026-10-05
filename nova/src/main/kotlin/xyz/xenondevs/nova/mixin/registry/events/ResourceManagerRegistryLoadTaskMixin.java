package xyz.xenondevs.nova.mixin.registry.events;

import com.mojang.serialization.Lifecycle;
import io.papermc.paper.registry.data.util.Conversions;
import net.minecraft.resources.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import xyz.xenondevs.nova.registry.RegistryEventManager;
import xyz.xenondevs.nova.registry.RegistryWriter;

import java.util.Map;
import java.util.stream.Stream;

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
    
    @ModifyArg(
        method = "lambda$load$3",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/resources/ResourceManagerRegistryLoadTask;registerElements(Ljava/util/stream/Stream;Lio/papermc/paper/registry/data/util/Conversions;)V"
        ),
        index = 0
    )
    private Stream<PendingRegistration<T>> preFreeze(
        Stream<PendingRegistration<T>> loadedEntries,
        Conversions conversions
    ) {
        nova$lookup = conversions.lookup();
        var entries = new RegistryWriter.Deferred<>(registry.key());
        RegistryEventManager.handlePreFreeze(entries, conversions.lookup());
        return Stream.concat(entries.stream(), loadedEntries);
    }
    
}
