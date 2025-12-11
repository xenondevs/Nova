package xyz.xenondevs.nova.mixin.registry.injectwrapperblock;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.registry.NovaRegistries;
import xyz.xenondevs.nova.world.generation.wrapper.WrapperBlock;

import java.util.Optional;

@Mixin(Registry.class)
interface RegistryMixin {
    
    // Mapping name will 100% change in the future, check for these params and method structure: https://i.imgur.com/4ix2zLq.png
    /**
     * Redirects a {@link Registry#get(Identifier)} call in Registry#referenceHolderWithLifecycle
     * to produce {@link WrapperBlock WrapperBlocks}.
     */
    @Redirect(
        method = "lambda$referenceHolderWithLifecycle$4",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/Registry;get(Lnet/minecraft/resources/Identifier;)Ljava/util/Optional;"
        )
    )
    private Optional<? extends Holder.Reference<?>> xd(Registry<?> registry, Identifier id) {
        Optional<? extends Holder.Reference<?>> holder = registry.get(id);
        if (holder.isEmpty() && registry.key() == Registries.BLOCK) {
            holder = NovaRegistries.WRAPPER_BLOCK.get(id);
        }
        return holder;
    }
    
}
