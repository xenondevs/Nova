package xyz.xenondevs.nova.mixin.block.pickitem;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.xenondevs.nova.world.block.NovaBlock;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerGamePacketListenerImplMixin {
    
    @WrapWithCondition(
        method = "handlePickItemFromBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;addBlockDataToItem(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private boolean skipNovaBlockEntityData(BlockState blockState, ServerLevel level, BlockPos pos, ItemStack itemStack) {
        return !(blockState.getBlock() instanceof NovaBlock);
    }
    
}
