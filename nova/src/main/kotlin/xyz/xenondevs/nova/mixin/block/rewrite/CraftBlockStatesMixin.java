package xyz.xenondevs.nova.mixin.block.rewrite;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.World;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.craftbukkit.block.CraftBlockStates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.world.block.NovaCapturedBlockEntityState;
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock;
import xyz.xenondevs.nova.world.block.NovaTileEntityProxy;

import java.util.Objects;

@Mixin(CraftBlockStates.class)
abstract class CraftBlockStatesMixin {
    
    @Inject(
        method = "getBlockState(Lorg/bukkit/World;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)Lorg/bukkit/craftbukkit/block/CraftBlockState;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void createNovaBlockEntityState(
        World world,
        BlockPos pos,
        BlockState state,
        BlockEntity blockEntity,
        CallbackInfoReturnable<CraftBlockState> cir
    ) {
        if (!(state.getBlock() instanceof NovaTileEntityBlock novaBlock))
            return;
        
        NovaTileEntityProxy proxy;
        if (blockEntity == null) {
            proxy = Objects.requireNonNull(novaBlock.newBlockEntity(pos, state));
        } else if (blockEntity instanceof NovaTileEntityProxy novaProxy) {
            proxy = novaProxy;
        } else {
            throw new IllegalStateException("Unexpected block entity for Nova block state: " + blockEntity);
        }
        
        cir.setReturnValue(new NovaCapturedBlockEntityState(world, proxy));
    }
    
}
