package xyz.xenondevs.nova.mixin.block.migration;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.util.NMSUtilsKt;
import xyz.xenondevs.nova.world.LevelChunkSectionFieldExtensionsKt;
import xyz.xenondevs.nova.world.block.migrator.BlockMigrator;

@Mixin(LevelChunk.class)
abstract class LevelChunkMixin {
    
    @Definition(id = "addAndRegisterBlockEntity", method = "Lnet/minecraft/world/level/chunk/LevelChunk;addAndRegisterBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;)V")
    @Expression("?.addAndRegisterBlockEntity(?)")
    @Redirect(method = "setBlockState", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void handleBlockEntityPlaced(LevelChunk chunk, BlockEntity blockEntity) {
        chunk.addAndRegisterBlockEntity(blockEntity);
        
        var section = chunk.getSection(chunk.getSectionIndex(blockEntity.getBlockPos().getY()));
        if (LevelChunkSectionFieldExtensionsKt.isMigrationActive(section)) {
            var novaPos = NMSUtilsKt.toNovaPos(blockEntity.getBlockPos(), chunk.getLevel().getWorld());
            BlockMigrator.handleBlockEntityPlaced(novaPos, blockEntity);
        }
    }
    
    @Definition(id = "removeBlockEntity", method = "Lnet/minecraft/world/level/chunk/LevelChunk;removeBlockEntity(Lnet/minecraft/core/BlockPos;)V")
    @Expression("?.removeBlockEntity(?)")
    @Redirect(method = "setBlockState", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void handleBlockEntityRemoved(LevelChunk chunk, BlockPos pos) {
        chunk.removeBlockEntity(pos);
        
        var section = chunk.getSection(chunk.getSectionIndex(pos.getY()));
        if (LevelChunkSectionFieldExtensionsKt.isMigrationActive(section)) {
            var novaPos = NMSUtilsKt.toNovaPos(pos, chunk.getLevel().getWorld());
            BlockMigrator.handleBlockEntityPlaced(novaPos, null);
        }
    }
    
}
