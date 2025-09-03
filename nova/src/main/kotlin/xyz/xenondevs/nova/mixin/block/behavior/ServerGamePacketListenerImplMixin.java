package xyz.xenondevs.nova.mixin.block.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.NovaBootstrapperKt;
import xyz.xenondevs.nova.context.Context;
import xyz.xenondevs.nova.context.intention.DefaultContextIntentions.BlockInteract;
import xyz.xenondevs.nova.context.param.DefaultContextParamTypes;
import xyz.xenondevs.nova.util.NMSUtilsKt;
import xyz.xenondevs.nova.world.format.WorldDataManager;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerGamePacketListenerImplMixin {
    
    @Shadow
    public ServerPlayer player;
    
    @Redirect(
        method = "handlePickItemFromBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getCloneItemStack(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private ItemStack handlePickItemFromBlock(BlockState state, LevelReader level, BlockPos pos, boolean includeData) {
        var novaPos = NMSUtilsKt.toNovaPos(pos, ((ServerLevel) level).getWorld());
        var novaState = WorldDataManager.INSTANCE.getBlockState(novaPos);
        if (novaState == null)
            return state.getCloneItemStack(level, pos, includeData);
        
        try {
            var paramTypes = DefaultContextParamTypes.INSTANCE;
            var ctx = Context.Companion.intention(BlockInteract.INSTANCE)
                .param(paramTypes.getBLOCK_POS(), novaPos)
                .param(paramTypes.getBLOCK_STATE_NOVA(), novaState)
                .param(paramTypes.getINCLUDE_DATA(), includeData)
                .param(paramTypes.getSOURCE_ENTITY(), player.getBukkitEntity())
                .build();
            return CraftItemStack.unwrap(novaState.getBlock().pickBlockCreative(novaPos, novaState, ctx));
        } catch (Exception e) {
            NovaBootstrapperKt.getLOGGER().error("Failed to get clone item stack for {} at {}", novaState, novaPos, e);
        }
        
        return ItemStack.EMPTY;
    }
    
}
