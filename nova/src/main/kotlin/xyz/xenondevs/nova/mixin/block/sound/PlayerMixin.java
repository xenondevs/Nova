package xyz.xenondevs.nova.mixin.block.sound;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

@SuppressWarnings("resource")
@Mixin(Player.class)
abstract class PlayerMixin {
    
    // TOOD: maybe?
    
//    @Redirect(
//        method = "playStepSound",
//        at = @At(
//            value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;playCombinationStepSounds(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V"
//        )
//    )
//    private void playCombinationStepSounds(
//        Player instance,
//        BlockState primaryState,
//        BlockState secondaryState,
//        @Local(ordinal = 0, argsOnly = true) BlockPos secondaryPos,
//        @Local(ordinal = 1) BlockPos primaryPos
//    ) {
//        nova$playStepSound(primaryPos, primaryState);
//        nova$playMuffledStepSound(secondaryPos, secondaryState);
//    }
//    
//    @Redirect(
//        method = "playStepSound",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/world/entity/Avatar;playStepSound(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"
//        )
//    )
//    private void playStepSound(Avatar entity, BlockPos pos, BlockState state) {
//        nova$playStepSound(pos, state);
//    }
//    
//    @Unique
//    private void nova$playStepSound(BlockPos pos, BlockState state) {
//        nova$playStepSound(pos, state, 0.15f, 1f);
//    }
//    
//    @Unique
//    private void nova$playMuffledStepSound(BlockPos pos, BlockState state) {
//        nova$playStepSound(pos, state, 0.05f, 0.8f);
//    }
//    
//    @Unique
//    private void nova$playStepSound(BlockPos pos, BlockState state, float volumeMultiplier, float pitchMultiplier) {
//        var player = (Player) (Object) this;
//        var vanillaSoundType = state.getSoundType();
//        var novaPos = NMSUtilsKt.toNovaPos(pos, player.level().getWorld());
//        var novaState = WorldDataManager.INSTANCE.getBlockState(novaPos);
//        
//        String oldSound = vanillaSoundType.getStepSound().location().getPath();
//        String newSound;
//        float volume;
//        float pitch;
//        
//        if (novaState != null) {
//            var sounds = novaState.getBlock().getBehaviorOrNull(BlockSounds.class);
//            if (sounds == null)
//                return;
//            var soundGroup = sounds.getSoundGroup();
//            
//            newSound = soundGroup.getStepSound();
//            volume = soundGroup.getVolume() * volumeMultiplier;
//            pitch = soundGroup.getPitch() * pitchMultiplier;
//        } else {
//            newSound = oldSound;
//            volume = vanillaSoundType.getVolume() * volumeMultiplier;
//            pitch = vanillaSoundType.getPitch() * pitchMultiplier;
//        }
//        
//        SoundEngine.broadcast(player, oldSound, newSound, volume, pitch);
//    }
    
}
