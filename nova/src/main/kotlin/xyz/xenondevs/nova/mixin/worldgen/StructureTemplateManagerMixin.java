package xyz.xenondevs.nova.mixin.worldgen;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.datafixers.DataFixer;
import net.minecraft.core.HolderGetter;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.loader.TemplateSource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.xenondevs.nova.world.generation.AddonStructureTemplateSource;

@Mixin(StructureTemplateManager.class)
abstract class StructureTemplateManagerMixin {
    
    @ModifyExpressionValue(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList$Builder;build()Lcom/google/common/collect/ImmutableList;"),
        require = 1
    )
    private ImmutableList<TemplateSource> addAddonTemplateSource(
        ImmutableList<TemplateSource> original,
        ResourceManager resourceManager,
        LevelStorageSource.LevelStorageAccess storage,
        DataFixer fixerUpper,
        HolderGetter<Block> blockLookup
    ) {
        return ImmutableList.<TemplateSource>builder()
            .addAll(original)
            .add(new AddonStructureTemplateSource(fixerUpper, blockLookup))
            .build();
    }
    
}
