package xyz.xenondevs.nova.mixin.block.vanillatileentity;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.xenondevs.nova.world.block.tileentity.vanilla.VanillaTileEntity;
import xyz.xenondevs.nova.world.block.tileentity.vanilla.VanillaTileEntityManager;

@Mixin(BlockEntity.class)
@SuppressWarnings("unused")
abstract class BlockEntityMixin {
    
    @Unique
    @Nullable
    public VanillaTileEntity nova$vanillaTileEntity;
    
    @Inject(method = "setRemoved", at = @At("RETURN"))
    private void handleVanillaTileEntityRemoved(CallbackInfo ci) {
        VanillaTileEntityManager.handleBlockEntityRemoved((BlockEntity) (Object) this);
    }
    
    @Inject(method = "clearRemoved", at = @At("RETURN"))
    private void handleVanillaTileEntityAdded(CallbackInfo ci) {
        VanillaTileEntityManager.handleBlockEntityAdded((BlockEntity) (Object) this);
    }
    
}
