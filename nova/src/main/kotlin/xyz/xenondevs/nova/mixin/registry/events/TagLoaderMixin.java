package xyz.xenondevs.nova.mixin.registry.events;

import io.papermc.paper.tag.TagEventConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.registry.RegistryEventManager;

import java.util.List;
import java.util.Map;

@Mixin(TagLoader.class)
abstract class TagLoaderMixin<T> {
    
    @Inject(method = "build", at = @At("HEAD"))
    private void build(
        Map<ResourceLocation, List<TagLoader.EntryWithSource>> builders,
        TagEventConfig<T, ?> eventConfig,
        CallbackInfoReturnable<Map<ResourceLocation, List<T>>> cir
    ) {
        RegistryEventManager.handleTagsBuild(builders, eventConfig);
    }
    
}
