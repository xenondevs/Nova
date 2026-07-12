package xyz.xenondevs.nova.mixin.block.rewrite;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import net.minecraft.core.IdMapper;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.commons.provider.Provider;
import xyz.xenondevs.nova.resources.lookup.ResourceLookups;
import xyz.xenondevs.nova.world.block.NovaBlock;
import xyz.xenondevs.nova.world.block.state.model.BackingStateBlockModelProvider;

import java.util.IdentityHashMap;
import java.util.Map;

@Mixin(IdMapper.class)
abstract class IdMapperMixin<T> {
    
    @Final
    @Shadow
    private Reference2IntMap<T> tToId;
    
    @Unique
    private final Provider<Map<BlockState, BlockState>> nova$maskedBlockStates = ResourceLookups.INSTANCE.getBlockModelLookup().map(lookup -> {
        var map = new IdentityHashMap<BlockState, BlockState>();
        for (var value : lookup.values()) {
            if (!(value instanceof BackingStateBlockModelProvider mp))
                continue;
            map.put(mp.getInfo().getVanillaBlockState(), mp.getInfo().getMaskedBlockState());
        }
        return map;
    });
    
    @Inject(method = "getId", at = @At("HEAD"), cancellable = true)
    private void modifyId(
        T thing,
        CallbackInfoReturnable<Integer> cir
    ) {
        if (!(thing instanceof BlockState state))
            return;
        
        var block = state.getBlock();
        BlockState replacement;
        if (block instanceof NovaBlock novaBlock) {
            replacement = novaBlock.getClientsideBlockStates().get(state);
        } else {
            replacement = nova$maskedBlockStates.get().get(state);
        }
        
        if (replacement != null) {
            cir.setReturnValue(tToId.getInt(replacement));
        }
    }
    
}
