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
import xyz.xenondevs.nova.data.world.WorldDataManager;
import xyz.xenondevs.nova.transformer.Patcher;
import xyz.xenondevs.nova.transformer.adapter.LcsWrapperAdapter;
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry;
import xyz.xenondevs.nova.util.reflection.ReflectionUtils;
import xyz.xenondevs.nova.world.generation.wrapper.WrapperBlockState;

import static xyz.xenondevs.nova.util.reflection.ReflectionRegistry.*;

/**
 * Wrapper for {@link LevelChunkSection}s to allow placing {@link WrapperBlockState}s.
 * <p>
 * <h2>! UPDATE {@link Patcher Patcher.injectedClasses} WHEN MOVING THIS CLASS !</h2>
 * <h2> Also check out {@link LcsWrapperAdapter} when refactoring </h2>
 */
public class LevelChunkSectionWrapper extends LevelChunkSection {
    
    // Vanilla
    private static final long COUNT_OFFSET = ReflectionUtils.getFieldOffset$nova(LEVEL_CHUNK_SECTION_NON_EMPTY_BLOCK_COUNT_FIELD);
    
    // Paper
    private static final long SPECIAL_COLLIDING_BLOCKS_OFFSET = LEVEL_CHUNK_SECTION_SPECIAL_COLLIDING_BLOCKS_FIELD == null ? -1 : ReflectionUtils.getFieldOffset$nova(LEVEL_CHUNK_SECTION_SPECIAL_COLLIDING_BLOCKS_FIELD);
    private static final long KNOWN_BLOCK_COLLISION_DATA_OFFSET = LEVEL_CHUNK_SECTION_KNOWN_BLOCK_COLLISION_DATA_FIELD == null ? -1 : ReflectionUtils.getFieldOffset$nova(LEVEL_CHUNK_SECTION_KNOWN_BLOCK_COLLISION_DATA_FIELD);
    private static final long TICKING_LIST_OFFSET = LEVEL_CHUNK_SECTION_TICKING_LIST_FIELD == null ? -1 : ReflectionUtils.getFieldOffset$nova(LEVEL_CHUNK_SECTION_TICKING_LIST_FIELD);
    
    // Pufferfish
    private static final long FLUID_STATE_COUNT_OFFSET = LEVEL_CHUNK_SECTION_FLUID_STATE_COUNT_FIELD == null ? -1 : ReflectionUtils.getFieldOffset$nova(LEVEL_CHUNK_SECTION_FLUID_STATE_COUNT_FIELD);
    
    private final Level level;
    private final ChunkPos chunkPos;
    private final int bottomBlockY;
    private final LevelChunkSection delegate;
    
    @SuppressWarnings("unchecked")
    public LevelChunkSectionWrapper(Level level, ChunkPos chunkPos, int bottomBlockY, LevelChunkSection delegate) throws IllegalAccessException {
        super(
            (PalettedContainer<BlockState>) ReflectionRegistry.INSTANCE.getLEVEL_CHUNK_SECTION_STATES_FIELD().get(delegate),
            (PalettedContainer<Holder<Biome>>) ReflectionRegistry.INSTANCE.getLEVEL_CHUNK_SECTION_BIOMES_FIELD().get(delegate)
        );
        this.level = level;
        this.chunkPos = chunkPos;
        this.bottomBlockY = bottomBlockY;
        this.delegate = delegate;
        recalcBlockCounts();
    }
    
    @Override
    public BlockState setBlockState(int relX, int relY, int relZ, BlockState state) {
        return setBlockState(relX, relY, relZ, state, true);
    }
    
    @Override
    public BlockState setBlockState(int relX, int relY, int relZ, BlockState state, boolean sync) {
        if (state instanceof WrapperBlockState wrappedState) {
            var chunkPos = this.chunkPos;
            WorldDataManager.INSTANCE.addOrphanBlock(level,
                relX + chunkPos.getMinBlockX(),
                relY + bottomBlockY,
                relZ + chunkPos.getMinBlockZ(),
                wrappedState.getNovaBlock());
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
    public void read(FriendlyByteBuf buf) {
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
        ReflectionUtils.putInt$nova(this, COUNT_OFFSET, ReflectionUtils.getInt$nova(delegate, COUNT_OFFSET));
        if (SPECIAL_COLLIDING_BLOCKS_OFFSET != -1) {
            ReflectionUtils.putInt$nova(this, SPECIAL_COLLIDING_BLOCKS_OFFSET, ReflectionUtils.getInt$nova(delegate, SPECIAL_COLLIDING_BLOCKS_OFFSET));
            ReflectionUtils.putReference$nova(this, KNOWN_BLOCK_COLLISION_DATA_OFFSET, ReflectionUtils.getReference$nova(delegate, KNOWN_BLOCK_COLLISION_DATA_OFFSET));
            ReflectionUtils.putReference$nova(this, TICKING_LIST_OFFSET, ReflectionUtils.getReference$nova(delegate, TICKING_LIST_OFFSET));
            
            if (FLUID_STATE_COUNT_OFFSET != -1) {
                ReflectionUtils.putInt$nova(this, FLUID_STATE_COUNT_OFFSET, ReflectionUtils.getInt$nova(delegate, FLUID_STATE_COUNT_OFFSET));
            }
        }
    }
    
}
