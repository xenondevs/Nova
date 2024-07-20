package xyz.xenondevs.nova.transformer.patch.worldgen.chunksection;

import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.nova.transformer.Patcher;
import xyz.xenondevs.nova.transformer.adapter.LcsWrapperAdapter;
import xyz.xenondevs.nova.util.reflection.ReflectionUtils;
import xyz.xenondevs.nova.world.BlockPos;
import xyz.xenondevs.nova.world.format.WorldDataManager;
import xyz.xenondevs.nova.world.generation.wrapper.WrapperBlockState;

import java.lang.reflect.Field;

/**
 * Wrapper for {@link LevelChunkSection}s to allow placing {@link WrapperBlockState}s.
 * <p>
 * <h2>! UPDATE {@link Patcher Patcher.injectedClasses} WHEN MOVING THIS CLASS !</h2>
 * <h2> Also check out {@link LcsWrapperAdapter} when refactoring </h2>
 */
public class LevelChunkSectionWrapper extends LevelChunkSection {
    
    private static final Field STATES =
        ReflectionUtils.getField(LevelChunkSection.class, "states");
    private static final Field BIOMES =
        ReflectionUtils.getField(LevelChunkSection.class, "biomes");
    private static final Field NON_EMPTY_BLOCK_COUNT =
        ReflectionUtils.getField(LevelChunkSection.class, "nonEmptyBlockCount");
    private static final Field TICKING_BLOCK_COUNT =
        ReflectionUtils.getField(LevelChunkSection.class, "tickingBlockCount");
    private static final Field TICKING_FLUID_COUNT =
        ReflectionUtils.getField(LevelChunkSection.class, "tickingFluidCount");
    private static final long NON_EMPTY_BLOCK_COUNT_OFFSET =
        ReflectionUtils.getFieldOffset$nova(NON_EMPTY_BLOCK_COUNT);
    private static final long TICKING_BLOCK_COUNT_OFFSET =
        ReflectionUtils.getFieldOffset$nova(TICKING_BLOCK_COUNT);
    private static final long TICKING_FLUID_COUNT_OFFSET =
        ReflectionUtils.getFieldOffset$nova(TICKING_FLUID_COUNT);
    
    private final Level level;
    private final ChunkPos chunkPos;
    private final int bottomBlockY;
    private final LevelChunkSection delegate;
    
    @SuppressWarnings("unchecked")
    public LevelChunkSectionWrapper(Level level, ChunkPos chunkPos, int bottomBlockY, LevelChunkSection delegate) throws IllegalAccessException {
        super(
            (PalettedContainer<BlockState>) STATES.get(delegate),
            (PalettedContainer<Holder<Biome>>) BIOMES.get(delegate)
        );
        this.level = level;
        this.chunkPos = chunkPos;
        this.bottomBlockY = bottomBlockY;
        this.delegate = delegate;
        recalcBlockCounts();
    }
    
    @Override
    public @NotNull BlockState setBlockState(int relX, int relY, int relZ, @NotNull BlockState state) {
        return setBlockState(relX, relY, relZ, state, true);
    }
    
    @Override
    public @NotNull BlockState setBlockState(int relX, int relY, int relZ, @NotNull BlockState state, boolean sync) {
        if (state instanceof WrapperBlockState wrappedState) {
            var chunkPos = this.chunkPos;
            WorldDataManager.INSTANCE.setBlockState(
                new BlockPos(
                    level.getWorld(),
                    relX + chunkPos.getMinBlockX(),
                    relY + bottomBlockY,
                    relZ + chunkPos.getMinBlockZ()
                ),
                wrappedState.getNovaState());
            return Blocks.AIR.defaultBlockState();
        }
        var blockState = delegate.setBlockState(relX, relY, relZ, state, sync);
        copyBlockCounts();
        return blockState;
    }
    
    @Override
    public void recalcBlockCounts() {
        if (delegate == null) return;
        delegate.recalcBlockCounts();
        copyBlockCounts();
    }
    
    @Override
    public void read(@NotNull FriendlyByteBuf buf) {
        delegate.read(buf);
        copyBlockCounts();
    }
    
    public Level getLevel() {
        return level;
    }
    
    public ChunkPos getChunkPos() {
        return chunkPos;
    }
    
    public int getBottomBlockY() {
        return bottomBlockY;
    }
    
    private void copyBlockCounts() {
        ReflectionUtils.putInt$nova(
            this,
            NON_EMPTY_BLOCK_COUNT_OFFSET,
            ReflectionUtils.getInt$nova(delegate, NON_EMPTY_BLOCK_COUNT_OFFSET)
        );
        ReflectionUtils.putInt$nova(
            this,
            TICKING_BLOCK_COUNT_OFFSET,
            ReflectionUtils.getInt$nova(delegate, TICKING_BLOCK_COUNT_OFFSET));
        ReflectionUtils.putInt$nova(
            this,
            TICKING_FLUID_COUNT_OFFSET,
            ReflectionUtils.getInt$nova(delegate, TICKING_FLUID_COUNT_OFFSET)
        );
    }
    
}
