package xyz.xenondevs.nova.mixin.block.sound;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.xenondevs.nova.util.BlockUtilsKt;
import xyz.xenondevs.nova.world.block.logic.PacketBlocks;
import xyz.xenondevs.nova.world.block.logic.sound.SoundEngine;

@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {
    
    @Inject(method = "levelEvent", at = @At("HEAD"))
    private void playerWillDestroy(
        Entity source,
        int type,
        BlockPos pos,
        int data,
        CallbackInfo ci
    ) {
        if (type != 2001) // block break
            return;
        
        var level = (ServerLevel) (Object) this;
        var block = level.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ());
        var soundGroup = BlockUtilsKt.getNovaSoundGroup(block);
        if (soundGroup == null)
            return;
        
        var volume = soundGroup.getBreakVolume();
        var serverBlockState = Block.BLOCK_STATE_REGISTRY.byId(data);
        var clientBlockState = serverBlockState != null
            ? PacketBlocks.getClientSideState(serverBlockState)
            : null;
        var oldSound = clientBlockState != null
            ? clientBlockState.getSoundType().getBreakSound().location().toString()
            : "";
        
        SoundEngine.broadcastIfOverridden(
            level,
            pos.getX() + 0.5,
            pos.getY() + 0.5,
            pos.getZ() + 0.5,
            volume > 1 ? 16 * volume : 16.0,
            oldSound,
            soundGroup.getBreakSound(),
            volume,
            soundGroup.getBreakPitch(),
            SoundSource.BLOCKS
        );
    }
    
}
