package xyz.xenondevs.nova.mixin.block.rewrite;

import com.google.common.base.Function;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.xenondevs.nova.world.block.NovaBlock;
import xyz.xenondevs.nova.world.block.NovaBlockStateImpl;
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock;

@Mixin(CraftBlockData.class)
abstract class CraftBlockDataMixin {
    
    @Inject(
        method = "<clinit>",
        at = @At(value = "INVOKE", target = "Lorg/bukkit/craftbukkit/block/data/CraftBlockData;reloadCache()V")
    )
    private static void registerNovaBlockData(CallbackInfo ci) {
        register(NovaBlock.class, NovaBlockStateImpl::new);
        register(NovaTileEntityBlock.class, NovaBlockStateImpl::new);
    }
    
    @Shadow
    private static void register(Class<? extends Block> blockClass, Function<BlockState, CraftBlockData> newInstance) {
    }
    
}
