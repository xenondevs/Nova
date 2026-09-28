package xyz.xenondevs.nova.mixin.block.vanillatileentity;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.bukkit.craftbukkit.block.CraftBlockStates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import xyz.xenondevs.nova.world.block.tileentity.vanilla.VanillaCauldronBlockEntity;

@Mixin(CraftBlockStates.class)
@SuppressWarnings("unused")
abstract class CraftBlockStatesMixin {
    
    @ModifyArg(
        method = "getBlockState(Lorg/bukkit/World;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)Lorg/bukkit/craftbukkit/block/CraftBlockState;",
        at = @At(
            value = "INVOKE",
            target = "Lorg/bukkit/craftbukkit/block/CraftBlockStates$BlockStateFactory;createBlockState(Lorg/bukkit/World;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)Lorg/bukkit/craftbukkit/block/CraftBlockState;"
        ),
        index = 3
    )
    private static BlockEntity hideCauldronBlockEntity(BlockEntity blockEntity) {
        return blockEntity instanceof VanillaCauldronBlockEntity ? null : blockEntity;
    }
    
}
