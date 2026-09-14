package xyz.xenondevs.nova.mixin.block.rewrite;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import net.minecraft.core.IdMapper;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.world.block.logic.PacketBlocks;

import static xyz.xenondevs.nova.network.PacketEncodingTrackingKt.isInPacketEncoding;

@Mixin(IdMapper.class)
abstract class IdMapperMixin<T> {
    
    @Final
    @Shadow
    public Reference2IntMap<T> tToId;
    
    @Inject(method = "getId", at = @At("HEAD"), cancellable = true)
    private void modifyId(
        T thing,
        CallbackInfoReturnable<Integer> cir
    ) {
        if (!isInPacketEncoding() || !(thing instanceof BlockState state))
            return;
        
        var replacement = PacketBlocks.getClientSideState(state);
        if (replacement != state)
            cir.setReturnValue(tToId.getInt(replacement));
    }
    
}
