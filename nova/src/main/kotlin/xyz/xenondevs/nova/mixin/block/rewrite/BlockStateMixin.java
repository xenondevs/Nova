package xyz.xenondevs.nova.mixin.block.rewrite;

import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.block.BlockType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import xyz.xenondevs.nova.registry.RegistryEntry;

// see BlockTypeAccess.kt
@Mixin(BlockState.class)
abstract class BlockStateMixin {
    
    @Unique
    @Nullable
    public BlockType nova$cachedType;
    
    @Unique
    @Nullable
    public RegistryEntry.Paper<BlockType> nova$cachedTypeEntry;
    
}
