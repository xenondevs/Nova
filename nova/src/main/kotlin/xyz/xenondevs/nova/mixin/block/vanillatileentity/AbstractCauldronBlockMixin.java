package xyz.xenondevs.nova.mixin.block.vanillatileentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import xyz.xenondevs.nova.world.block.tileentity.vanilla.VanillaCauldronBlockEntity;

@Mixin(AbstractCauldronBlock.class)
@NullMarked
@SuppressWarnings("unused")
abstract class AbstractCauldronBlockMixin implements EntityBlock {
    
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (!nova$isSupportedCauldron(state.getBlock()))
            return null;
        
        return new VanillaCauldronBlockEntity(pos, state);
    }
    
    // Overrides the inherited BlockBehaviour method for transitions between supported cauldron blocks.
    protected boolean shouldChangedStateKeepBlockEntity(BlockState oldState) {
        return nova$isSupportedCauldron((Block) (Object) this)
               && nova$isSupportedCauldron(oldState.getBlock());
    }
    
    @Unique
    private static boolean nova$isSupportedCauldron(Block block) {
        return block == Blocks.CAULDRON
               || block == Blocks.WATER_CAULDRON
               || block == Blocks.LAVA_CAULDRON;
    }
    
}
