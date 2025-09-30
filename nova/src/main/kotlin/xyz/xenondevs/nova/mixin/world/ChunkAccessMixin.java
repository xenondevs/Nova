package xyz.xenondevs.nova.mixin.world;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.xenondevs.nova.world.LevelChunkSectionFieldExtensionsKt;

@Mixin(ChunkAccess.class)
abstract class ChunkAccessMixin {
    
    @Inject(
        method = "<init>",
        at = @At(value = "TAIL")
    )
    private void inject(
        ChunkPos chunkPos,
        UpgradeData upgradeData,
        LevelHeightAccessor levelHeightAccessor,
        PalettedContainerFactory palettedContainerFactory,
        long inhabitedTime,
        LevelChunkSection[] sections,
        BlendingData blendingData,
        CallbackInfo ci
    ) {
        var actualSections = ((ChunkAccess) (Object) this).sections;
        for (int i = 0; i < actualSections.length; i++) {
            var section = actualSections[i];
            LevelChunkSectionFieldExtensionsKt.setLevel(section, (Level) levelHeightAccessor);
            LevelChunkSectionFieldExtensionsKt.setChunkPos(section, chunkPos);
            LevelChunkSectionFieldExtensionsKt.setBottomBlockY(section, (levelHeightAccessor.getMinSectionY() + i) * 16);
        }
    }
    
}
