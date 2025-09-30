package xyz.xenondevs.nova.mixin.block.disablelogic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@SuppressWarnings("OverwriteAuthorRequired")
@Mixin(NoteBlock.class)
abstract class NoteBlockMixin {
    
    @Overwrite
    protected BlockState updateShape(
        BlockState state,
        LevelReader level,
        ScheduledTickAccess tickAccess,
        BlockPos pos,
        Direction direction,
        BlockPos neighborPos,
        BlockState neighborState,
        RandomSource randomSource
    ) {
        return state;
    }
    
    @Overwrite
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        Orientation orientation,
        boolean movedByPiston
    ) {
    }
    
    @Overwrite
    protected InteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        return InteractionResult.FAIL;
    }
    
    @Overwrite
    protected InteractionResult useWithoutItem(
        BlockState state, 
        Level level, 
        BlockPos pos,
        Player player,
        BlockHitResult hitResult
    ) {
        return InteractionResult.PASS;
    }
    
    @Overwrite
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
    }
    
    @Overwrite
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        return false;
    }

}    
    
    
