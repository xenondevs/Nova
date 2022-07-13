package xyz.xenondevs.nova.transformer.patch.noteblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.Bukkit;

class NoteBlockMethods {
    
    private void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos neighborPos, boolean flag) {
        try {
            var loader = Bukkit.getPluginManager().getPlugin("Nova").getClass().getClassLoader();
            var clazz = loader.loadClass("xyz.xenondevs.nova.world.block.behavior.noteblock.AgentNoteBlockBehavior");
            var neighborChanged = clazz.getMethod("neighborChanged", BlockState.class, Level.class, BlockPos.class, Block.class, BlockPos.class, boolean.class);
            neighborChanged.invoke(null, state, level, pos, block, neighborPos, flag);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult result) {
        try {
            var loader = Bukkit.getPluginManager().getPlugin("Nova").getClass().getClassLoader();
            var clazz = loader.loadClass("xyz.xenondevs.nova.world.block.behavior.noteblock.AgentNoteBlockBehavior");
            var neighborChanged = clazz.getMethod("use", BlockState.class, Level.class, BlockPos.class, Player.class, InteractionHand.class, BlockHitResult.class);
            return (InteractionResult) neighborChanged.invoke(null, state, level, pos, player, hand, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    private void attack(BlockState state, Level level, BlockPos pos, Player player) {
        try {
            var loader = Bukkit.getPluginManager().getPlugin("Nova").getClass().getClassLoader();
            var clazz = loader.loadClass("xyz.xenondevs.nova.world.block.behavior.noteblock.AgentNoteBlockBehavior");
            var neighborChanged = clazz.getMethod("attack", BlockState.class, Level.class, BlockPos.class, Player.class);
            neighborChanged.invoke(null, state, level, pos, player);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private boolean triggerEvent(BlockState state, Level level, BlockPos pos, int i, int j) {
        return true;
    }
    
}
