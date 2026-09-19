package xyz.xenondevs.nova.mixin.block.bukkit;

import com.llamalad7.mixinextras.sugar.Local;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftBlockState.class)
abstract class CraftBlockStateMixin {
    
    @Shadow
    public net.minecraft.world.level.block.state.BlockState block;
    
    @Inject(
        method = "update(ZZ)Z",
        at = @At(
            value = "INVOKE",
            target = "Lorg/bukkit/craftbukkit/block/CraftBlock;getType()Lorg/bukkit/Material;"
        ),
        cancellable = true
    )
    private void preventUpdateIfBlockTypeChanged(
        boolean force,
        boolean applyPhysics,
        CallbackInfoReturnable<Boolean> cir,
        @Local(name = "block") CraftBlock block
    ) {
        if (!force && block.getBlockState().getBlock() != this.block.getBlock())
            cir.setReturnValue(false);
    }
    
}
