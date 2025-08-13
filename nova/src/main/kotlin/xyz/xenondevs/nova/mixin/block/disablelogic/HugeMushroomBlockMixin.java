package xyz.xenondevs.nova.mixin.block.disablelogic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@SuppressWarnings("OverwriteAuthorRequired")
@Mixin(HugeMushroomBlock.class)
abstract class HugeMushroomBlockMixin {
    
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
    
    @Overwrite
    protected BlockState rotate(BlockState state, Rotation rot) {
        return ((Block)(Object)this).defaultBlockState();
    }
    
    @Overwrite
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return ((Block)(Object)this).defaultBlockState();
    }
    
}
