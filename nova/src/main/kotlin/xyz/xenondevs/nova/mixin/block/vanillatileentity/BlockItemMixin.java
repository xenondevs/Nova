package xyz.xenondevs.nova.mixin.block.vanillatileentity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.xenondevs.nova.world.block.tileentity.vanilla.VanillaTileEntityManager;

@Mixin(BlockItem.class)
@SuppressWarnings("unused")
abstract class BlockItemMixin {
    
    @WrapOperation(
        method = "place",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/BlockItem;placeBlock(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z"
        )
    )
    private boolean capturePlacementContext(
        BlockItem instance,
        BlockPlaceContext context,
        BlockState placementState,
        Operation<Boolean> original
    ) {
        return VanillaTileEntityManager.withBlockPlacement(
            context,
            () -> original.call(instance, context, placementState)
        );
    }
    
}
