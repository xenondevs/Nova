package xyz.xenondevs.nova.mixin.block.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.world.block.migrator.BlockMigrator;

@Mixin(Block.class)
abstract class BlockMixin {
    
    @Redirect(
        method = "updateOrDestroy(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;II)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z"
        )
    )
    private static boolean setBlockWithoutMigrations(LevelAccessor level, BlockPos pos, BlockState newState, int flags, int recursionLeft) {
        BlockMigrator.migrationSuppression.set(BlockMigrator.migrationSuppression.get() + 1);
        try {
            level.setBlock(pos, newState, flags & -33, recursionLeft);
        } finally {
            BlockMigrator.migrationSuppression.set(BlockMigrator.migrationSuppression.get() - 1);
        }
        return false;
    }
    
}
