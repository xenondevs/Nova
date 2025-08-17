package xyz.xenondevs.nova.mixin.block.wrapperblock;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.world.generation.wrapper.WrapperBlock;

/**
 * Prevents {@link WrapperBlock WrapperBlocks} from creating intrusive holders in the block registry.
 */
@Mixin(Block.class)
abstract class BlockMixin {
    
    @Definition(id = "BLOCK", field = "Lnet/minecraft/core/registries/BuiltInRegistries;BLOCK:Lnet/minecraft/core/DefaultedRegistry;")
    @Definition(id = "createIntrusiveHolder", method = "Lnet/minecraft/core/DefaultedRegistry;createIntrusiveHolder(Ljava/lang/Object;)Lnet/minecraft/core/Holder$Reference;")
    @Expression("BLOCK.createIntrusiveHolder(this)")
    @Redirect(method = "<init>", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
    private Holder.Reference<Block> createIntrusiveHolder(DefaultedRegistry<Block> instance, Object o) {
        if (o instanceof WrapperBlock)
            return null;
        return instance.createIntrusiveHolder((Block) o);
    }
    
}
