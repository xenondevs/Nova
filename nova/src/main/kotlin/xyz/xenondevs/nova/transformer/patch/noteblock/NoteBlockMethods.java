package xyz.xenondevs.nova.transformer.patch.noteblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import xyz.xenondevs.nova.world.block.behavior.impl.noteblock.AgentNoteBlockBehavior;

class NoteBlockMethods {
    
    private void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos neighborPos, boolean flag) {
        AgentNoteBlockBehavior.neighborChanged(state, level, pos, block, neighborPos, flag);
    }
    
    private InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult result) {
        return AgentNoteBlockBehavior.use(state, level, pos, player, hand, result);
    }
    
    private void attack(BlockState state, Level level, BlockPos pos, Player player) {
        AgentNoteBlockBehavior.attack(state, level, pos, player);
    }
    
    private boolean triggerEvent(BlockState state, Level level, BlockPos pos, int i, int j) {
        return true;
    }
    
}
