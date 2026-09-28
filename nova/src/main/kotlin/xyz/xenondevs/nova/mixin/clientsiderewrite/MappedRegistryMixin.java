package xyz.xenondevs.nova.mixin.clientsiderewrite;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import net.minecraft.core.MappedRegistry;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.world.block.NovaBlock;
import xyz.xenondevs.nova.world.block.logic.PacketBlocks;
import xyz.xenondevs.nova.world.item.NovaItem;

import static xyz.xenondevs.nova.network.PacketEncodingTrackingKt.isInPacketEncoding;

@Mixin(MappedRegistry.class)
abstract class MappedRegistryMixin<T> {
    
    @Final
    @Shadow
    private Reference2IntMap<T> toId;
    
    @Inject(method = "getId", at = @At("HEAD"), cancellable = true)
    private void modifyGetId(
        @Nullable T thing,
        CallbackInfoReturnable<Integer> cir
    ) {
        if (!isInPacketEncoding())
            return;
        
        if (thing instanceof NovaBlock novaBlock)
            cir.setReturnValue(toId.getInt(PacketBlocks.getClientSideBlock(novaBlock)));
        
        // fall back to plain shulker shell in case something is not handled by PacketItems
        if (thing instanceof NovaItem)
            cir.setReturnValue(toId.getInt(Items.SHULKER_SHELL));
    }
    
}
