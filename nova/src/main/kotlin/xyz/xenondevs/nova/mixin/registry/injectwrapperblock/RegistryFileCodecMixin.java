package xyz.xenondevs.nova.mixin.registry.injectwrapperblock;

import com.mojang.serialization.DynamicOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.registry.NovaRegistries;
import xyz.xenondevs.nova.world.generation.wrapper.WrapperBlock;

import java.util.Optional;

@SuppressWarnings({"unchecked", "rawtypes"})
@Mixin(RegistryFileCodec.class)
abstract class RegistryFileCodecMixin {
   
   /**
    * Replaces the holder getter used in {@link RegistryFileCodec#decode(DynamicOps, Object)}
    * to a one that can produce {@link WrapperBlock WrapperBlocks} for Nova blocks.
    */
   @Redirect(
       method = "decode",
       at = @At(
           value = "INVOKE", 
           target ="Lnet/minecraft/resources/RegistryOps;getter(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;"
       )
   )
   private Optional<? extends HolderGetter<?>> getter(
       RegistryOps<?> ops,
       ResourceKey<? extends Registry<?>> key
   ) {
      if (key != Registries.BLOCK)
         return ops.getter(key);
      
      var getter = ops.getter(Registries.BLOCK).orElseThrow();
      var injectGetter = new HolderGetter<Block>() {
         
         @Override
         public @NotNull Optional<Holder.Reference<Block>> get(@NotNull ResourceKey<Block> key) {
            var vanilla = getter.get(key);
            if (vanilla.isPresent())
               return vanilla;
            return (Optional) NovaRegistries.WRAPPER_BLOCK.get(key.location());
         }
         
         @Override
         public @NotNull Optional<HolderSet.Named<Block>> get(@NotNull TagKey<Block> tag) {
            return getter.get(tag);
         }
         
      };
      
      return Optional.of(injectGetter);
   }
    
}
