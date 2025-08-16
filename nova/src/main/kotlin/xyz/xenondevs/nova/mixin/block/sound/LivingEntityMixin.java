package xyz.xenondevs.nova.mixin.block.sound;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import xyz.xenondevs.nova.util.BlockUtilsKt;
import xyz.xenondevs.nova.util.item.MaterialUtilsKt;
import xyz.xenondevs.nova.world.BlockPos;
import xyz.xenondevs.nova.world.block.logic.sound.SoundEngine;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    
    @SuppressWarnings({"OverwriteAuthorRequired", "resource", "removal"})
    @Overwrite
    protected void playBlockFallSound() {
        var entity = (LivingEntity) (Object) this;
        if (entity.isSilent())
            return;
        
        int x = Mth.floor(entity.getX());
        int y = Mth.floor(entity.getY() - 0.2f);
        int z = Mth.floor(entity.getZ());
        
        var novaPos = new BlockPos(entity.level().getWorld(), x, y, z);
        var block = novaPos.getBlock();
        if (block.getType().isAir())
            return;
        
        var soundGroup = BlockUtilsKt.getNovaSoundGroup(block);
        if (soundGroup == null)
            return;
        
        var newSound = soundGroup.getFallSound();
        var oldSound = MaterialUtilsKt.getSoundGroup(block.getType()).getFallSound().getKey().value();
        SoundEngine.broadcast(entity, oldSound, newSound, soundGroup.getFallVolume(), soundGroup.getFallPitch());
    }
    
}
