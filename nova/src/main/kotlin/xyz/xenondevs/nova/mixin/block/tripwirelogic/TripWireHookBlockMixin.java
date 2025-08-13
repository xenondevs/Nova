package xyz.xenondevs.nova.mixin.block.tripwirelogic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.util.NMSUtilsKt;
import xyz.xenondevs.nova.world.block.DefaultBlocks;
import xyz.xenondevs.nova.world.block.behavior.TripwireBehavior;
import xyz.xenondevs.nova.world.format.WorldDataManager;

@Mixin(TripWireHookBlock.class)
abstract class TripWireHookBlockMixin {
    
    @Redirect(
        method = "calculateState",
        at = @At(
            value = "INVOKE",
            target ="Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
        )
    )
    private static BlockState inject(Level level, BlockPos pos) {
        var novaPos = NMSUtilsKt.toNovaPos(pos, level.getWorld());
        var novaState = WorldDataManager.INSTANCE.getBlockState(novaPos);
        if (novaState != null && novaState.getBlock() == DefaultBlocks.INSTANCE.getTRIPWIRE()) {
            return TripwireBehavior.INSTANCE.vanillaBlockStateOf(novaState);
        }
        
        return level.getBlockState(pos);
    }
    
}
