package xyz.xenondevs.nova.transformer.patch.worldgen.chunksection;

import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.material.FluidState;
import xyz.xenondevs.nova.data.world.WorldDataManager;
import xyz.xenondevs.nova.transformer.Patcher;
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry;
import xyz.xenondevs.nova.util.reflection.ReflectionUtils;
import xyz.xenondevs.nova.world.generation.wrapper.WrapperBlockState;

import java.util.function.Predicate;

import static xyz.xenondevs.nova.util.reflection.ReflectionRegistry.*;

/**
 * Wrapper for {@link LevelChunkSection}s to allow placing {@link WrapperBlockState}s.
 * <p>
 * <h2>! UPDATE {@link Patcher Patcher.injectedClasses} WHEN MOVING THIS CLASS !</h2>
 */
public class LevelChunkSectionWrapper extends LevelChunkSection {
    
    private static final long COUNT_OFFSET = ReflectionUtils.getFieldOffset$nova(LEVEL_CHUNK_SECTION_NON_EMPTY_BLOCK_COUNT_FIELD);
    private static final long SPECIAL_COLLIDING_BLOCKS_OFFSET = LEVEL_CHUNK_SECTION_SPECIAL_COLLIDING_BLOCKS_FIELD == null ? - 1 : ReflectionUtils.getFieldOffset$nova(LEVEL_CHUNK_SECTION_SPECIAL_COLLIDING_BLOCKS_FIELD);
    private static final long KNOWN_BLOCK_COLLISION_DATA_OFFSET = LEVEL_CHUNK_SECTION_KNOWN_BLOCK_COLLISION_DATA_FIELD == null ? - 1 : ReflectionUtils.getFieldOffset$nova(LEVEL_CHUNK_SECTION_KNOWN_BLOCK_COLLISION_DATA_FIELD);
    
    private final Level level;
    private final ChunkPos chunkPos;
    private final LevelChunkSection delegate;
    
    @SuppressWarnings("unchecked")
    public LevelChunkSectionWrapper(Level level, ChunkPos chunkPos, LevelChunkSection delegate) throws IllegalAccessException {
        super(
            delegate.bottomBlockY() >> 4,
            (PalettedContainer<BlockState>) ReflectionRegistry.INSTANCE.getLEVEL_CHUNK_SECTION_STATES_FIELD().get(delegate),
            (PalettedContainer<Holder<Biome>>) ReflectionRegistry.INSTANCE.getLEVEL_CHUNK_SECTION_BIOMES_FIELD().get(delegate)
        );
        this.level = level;
        this.chunkPos = chunkPos;
        this.delegate = delegate;
        recalcBlockCounts();
    }
    
    @Override
    public BlockState getBlockState(int i, int j, int k) {
        return delegate.getBlockState(i, j, k);
    }
    
    @Override
    public FluidState getFluidState(int i, int j, int k) {
        return delegate.getFluidState(i, j, k);
    }
    
    @Override
    public void acquire() {
        delegate.acquire();
    }
    
    @Override
    public void release() {
        delegate.release();
    }
    
    @Override
    public BlockState setBlockState(int relX, int relY, int relZ, BlockState state) {
        return setBlockState(relX, relY, relZ, state, true);
    }
    
    @Override
    public BlockState setBlockState(int relX, int relY, int relZ, BlockState state, boolean sync) {
        if (state instanceof WrapperBlockState wrappedState) {
            var chunkPos = this.chunkPos;
            WorldDataManager.INSTANCE.addOrphanBlock$nova(level,
                relX + chunkPos.getMinBlockX(),
                relY + bottomBlockY(),
                relZ + chunkPos.getMinBlockZ(),
                wrappedState.getNovaBlock());
            return Blocks.AIR.defaultBlockState();
        }
        var blockState = delegate.setBlockState(relX, relY, relZ, state, sync);
        copyBlockCounts();
        return blockState;
    }
    
    @Override
    public boolean hasOnlyAir() {
        return delegate.hasOnlyAir();
    }
    
    @Override
    public boolean isRandomlyTicking() {
        return delegate.isRandomlyTicking();
    }
    
    @Override
    public boolean isRandomlyTickingBlocks() {
        return delegate.isRandomlyTickingBlocks();
    }
    
    @Override
    public boolean isRandomlyTickingFluids() {
        return delegate.isRandomlyTickingFluids();
    }
    
    @Override
    public int bottomBlockY() {
        return delegate.bottomBlockY();
    }
    
    @Override
    public void recalcBlockCounts() {
        if (delegate == null) return;
        delegate.recalcBlockCounts();
        copyBlockCounts();
    }
    
    @Override
    public PalettedContainer<BlockState> getStates() {
        return delegate.getStates();
    }
    
    @Override
    public PalettedContainerRO<Holder<Biome>> getBiomes() {
        return delegate.getBiomes();
    }
    
    @Override
    public void read(FriendlyByteBuf packetdataserializer) {
        delegate.read(packetdataserializer);
        copyBlockCounts();
    }
    
    @Override
    public void write(FriendlyByteBuf packetdataserializer) {
        delegate.write(packetdataserializer);
    }
    
    @Override
    public int getSerializedSize() {
        return delegate.getSerializedSize();
    }
    
    @Override
    public boolean maybeHas(Predicate<BlockState> predicate) {
        return delegate.maybeHas(predicate);
    }
    
    @Override
    public Holder<Biome> getNoiseBiome(int i, int j, int k) {
        return delegate.getNoiseBiome(i, j, k);
    }
    
    @Override
    public void setBiome(int i, int j, int k, Holder<Biome> biome) {
        delegate.setBiome(i, j, k, biome);
    }
    
    @Override
    public void fillBiomesFromNoise(BiomeResolver biomeresolver, Climate.Sampler climate_sampler, int i, int j) {
        delegate.fillBiomesFromNoise(biomeresolver, climate_sampler, i, j);
    }
    
    public Level getLevel() {
        return level;
    }
    
    private void copyBlockCounts() {
        ReflectionUtils.putInt$nova(this, COUNT_OFFSET, ReflectionUtils.getInt$nova(delegate, COUNT_OFFSET));
        if (SPECIAL_COLLIDING_BLOCKS_OFFSET != - 1) {
            ReflectionUtils.putInt$nova(this, SPECIAL_COLLIDING_BLOCKS_OFFSET, ReflectionUtils.getInt$nova(delegate, SPECIAL_COLLIDING_BLOCKS_OFFSET));
            ReflectionUtils.putReference$nova(this, KNOWN_BLOCK_COLLISION_DATA_OFFSET, ReflectionUtils.getReference$nova(delegate, KNOWN_BLOCK_COLLISION_DATA_OFFSET));
        }
    }
    
}
