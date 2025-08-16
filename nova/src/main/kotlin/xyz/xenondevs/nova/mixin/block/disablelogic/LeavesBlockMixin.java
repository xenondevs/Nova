package xyz.xenondevs.nova.mixin.block.disablelogic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@SuppressWarnings("OverwriteAuthorRequired")
@Mixin(LeavesBlock.class)
abstract class LeavesBlockMixin {
    
    @Overwrite
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    }
    
    @Overwrite
    protected boolean isRandomlyTicking(BlockState state) {
        return false;
    }
    
    @Overwrite
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    }
    
    @Overwrite
    protected BlockState updateShape(
        BlockState state,
        LevelReader level,
        ScheduledTickAccess scheduledTickAccess,
        BlockPos pos,
        Direction direction,
        BlockPos neighborPos,
        BlockState neighborState,
        RandomSource random
    ) {
        return state;
    }
    
}
