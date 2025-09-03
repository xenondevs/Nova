package xyz.xenondevs.nova.mixin.block.earlyplaceevent;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.NovaBootstrapperKt;
import xyz.xenondevs.nova.util.NMSUtilsKt;
import xyz.xenondevs.nova.world.block.logic.place.BlockPlacing;

@Mixin(BlockItem.class)
abstract class BlockItemMixin {
    
    @Redirect(
        method = "place",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/BlockItem;placeBlock(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z"
        )
    )
    private boolean placeBlock(BlockItem item, BlockPlaceContext context, BlockState state) {
        try {
            var pos = NMSUtilsKt.toNovaPos(context.getClickedPos(), context.getLevel().getWorld());
            if (!BlockPlacing.INSTANCE.handleBlockPlace(pos)) {
                var player = context.getPlayer();
                if (player != null)
                    player.containerMenu.sendAllDataToRemote();
                return false;
            }
        } catch (Throwable t) {
            NovaBootstrapperKt.getLOGGER().error("An exception occurred while handling early block place event", t);
        }
        return item.placeBlock(context, state);
    }
    
}
