package xyz.xenondevs.nova.mixin.world;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.world.BlockPos;
import xyz.xenondevs.nova.world.block.migrator.BlockMigrator;
import xyz.xenondevs.nova.world.format.WorldDataManager;
import xyz.xenondevs.nova.world.generation.wrapper.WrapperBlockState;

@Mixin(value = LevelChunkSection.class)
abstract class LevelChunkSectionMixin {
    
    @Unique
    private Level nova$level;
    @Unique
    private ChunkPos nova$chunkPos;
    @Unique
    private int nova$bottomBlockY;
    @Unique
    private boolean nova$migrationActive;
    
    @Inject(
        method = "setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
        at = @At("HEAD"),
        order = 0,
        cancellable = true
    )
    private void handleWrapperBlockState(
        int x,
        int y,
        int z,
        BlockState state,
        boolean useLocks,
        CallbackInfoReturnable<BlockState> cir
    ) {
        var pos = nova$getPos(x, y, z);
        if (state instanceof WrapperBlockState wrappedState) {
            WorldDataManager.INSTANCE.setBlockState(pos, wrappedState.getNovaState());
            cir.setReturnValue(Blocks.AIR.defaultBlockState());
        }
    }
    
    @ModifyVariable(
        method = "setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
        at = @At("HEAD"),
        argsOnly = true,
        order = 1
    )
    private BlockState migrateBlockState(
        BlockState blockState,
        @Local(ordinal = 0, argsOnly = true) int x,
        @Local(ordinal = 1, argsOnly = true) int y,
        @Local(ordinal = 2, argsOnly = true) int z,
        @Share("unmigrated") LocalRef<BlockState> unmigrated
    ) {
        unmigrated.set(blockState);
        if (!nova$migrationActive)
            return blockState;
        return BlockMigrator.migrateBlockState(nova$getPos(x, y, z), blockState);
    }
    
    @Inject(
        method = "setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
        at = @At("TAIL")
    )
    private void handleBlockStatePlaced(
        int x, int y, int z,
        BlockState state,
        boolean useLocks,
        CallbackInfoReturnable<BlockState> cir,
        @Share("unmigrated") LocalRef<BlockState> unmigrated,
        @Local(ordinal = 1) BlockState previous
    ) {
        if (!nova$migrationActive)
            return;
        BlockMigrator.handleBlockStatePlaced(nova$getPos(x, y, z), previous, unmigrated.get());
    }
    
    @Unique
    private BlockPos nova$getPos(int x, int y, int z) {
        return new BlockPos(
            nova$level.getWorld(),
            x + nova$chunkPos.getMinBlockX(),
            y + nova$bottomBlockY,
            z + nova$chunkPos.getMinBlockZ()
        );
    }
    
}
