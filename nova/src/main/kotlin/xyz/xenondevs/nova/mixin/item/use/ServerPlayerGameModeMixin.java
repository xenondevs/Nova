package xyz.xenondevs.nova.mixin.item.use;

import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.util.NMSUtilsKt;
import xyz.xenondevs.nova.util.item.ItemUtilsKt;
import xyz.xenondevs.nova.world.format.WorldDataManager;

@Mixin(ServerPlayerGameMode.class)
abstract class ServerPlayerGameModeMixin {
    
    @Redirect(
        method = "useItemOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;useItemOn(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"
        )
    )
    private InteractionResult blockStateUseItemOn(
        BlockState blockState,
        ItemStack itemStack,
        Level level,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        var novaPos = NMSUtilsKt.toNovaPos(hitResult.getBlockPos(), level.getWorld());
        var novaState = WorldDataManager.INSTANCE.getBlockState(novaPos);
        if (novaState == null)
            return blockState.useItemOn(itemStack, level, player, hand, hitResult);
        return novaState.getBlock().useItemOnNms$nova(novaPos, novaState, itemStack.copy(), player, hand, hitResult);
    }
    
    @Redirect(
        method = "useItemOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;useWithoutItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"
        )
    )
    private InteractionResult blockStateUseWithoutItem(
        BlockState blockState,
        Level level,
        Player player,
        BlockHitResult hitResult
    ) {
        var novaPos = NMSUtilsKt.toNovaPos(hitResult.getBlockPos(), level.getWorld());
        var novaState = WorldDataManager.INSTANCE.getBlockState(novaPos);
        if (novaState == null)
            return blockState.useWithoutItem(level, player, hitResult);
        return novaState.getBlock().useNms$nova(novaPos, novaState, player, hitResult);
    }
    
    @Redirect(
        method = "useItemOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"
        )
    )
    private InteractionResult itemStackUseOn(
        ItemStack itemStack,
        UseOnContext useOnContext
    ) {
        var novaItem = ItemUtilsKt.getNovaItem(itemStack);
        if (novaItem == null)
            return itemStack.useOn(useOnContext);
        return novaItem.useOnBlockNms$nova(itemStack.copy(), useOnContext);
    }
    
    @Redirect(
        method = "useItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"
        )
    )
    private InteractionResult itemStackUse(
        ItemStack itemStack,
        Level level,
        Player player,
        InteractionHand hand
    ) {
        var novaItem = ItemUtilsKt.getNovaItem(itemStack);
        if (novaItem == null)
            return itemStack.use(level, player, hand);
        return novaItem.useNms$nova(itemStack.copy(), player, hand);
    }
    
}
