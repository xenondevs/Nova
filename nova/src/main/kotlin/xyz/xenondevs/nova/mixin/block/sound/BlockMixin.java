package xyz.xenondevs.nova.mixin.block.sound;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.util.BlockUtilsKt;
import xyz.xenondevs.nova.util.NMSUtilsKt;
import xyz.xenondevs.nova.util.item.MaterialUtilsKt;
import xyz.xenondevs.nova.world.block.logic.sound.SoundEngine;

@Mixin(Block.class)
abstract class BlockMixin {
    
    @SuppressWarnings("removal")
    @Inject(method = "playerWillDestroy", at = @At("TAIL"))
    private void playerWillDestroy(
        Level level,
        BlockPos pos,
        BlockState state,
        Player player,
        CallbackInfoReturnable<BlockState> cir
    ) {
        var novaPos = NMSUtilsKt.toNovaPos(pos, level.getWorld());
        var soundGroup = BlockUtilsKt.getNovaSoundGroup(novaPos.getBlock());
        if (soundGroup == null)
            return;
        
        var volume = soundGroup.getBreakVolume();
        var pitch = soundGroup.getBreakPitch();
        var oldSound = MaterialUtilsKt.getSoundGroup(novaPos.getBlock().getType()).getBreakSound().key().value();
        SoundEngine.broadcastIfOverridden(
            level,
            pos.getX() + 0.5, 
            pos.getY() + 0.5, 
            pos.getZ() + 0.5,
            volume > 1 ? 16 * volume : 16.0,
            oldSound, 
            soundGroup.getBreakSound(),
            volume, pitch, 
            SoundSource.BLOCKS
        );
    }
    
}
