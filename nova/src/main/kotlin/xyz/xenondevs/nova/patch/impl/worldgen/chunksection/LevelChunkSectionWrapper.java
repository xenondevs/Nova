package xyz.xenondevs.nova.patch.impl.worldgen.chunksection;

import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.nova.patch.Patcher;
import xyz.xenondevs.nova.patch.adapter.LcsWrapperAdapter;
import xyz.xenondevs.nova.util.reflection.ReflectionUtils;
import xyz.xenondevs.nova.world.BlockPos;
import xyz.xenondevs.nova.world.block.migrator.BlockMigrator;
import xyz.xenondevs.nova.world.format.WorldDataManager;
import xyz.xenondevs.nova.world.generation.wrapper.WrapperBlockState;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

/**
 * Wrapper for {@link LevelChunkSection}s to allow placing {@link WrapperBlockState}s.
 * <p>
 * <h2>! UPDATE {@link Patcher Patcher.injectedClasses} WHEN MOVING THIS CLASS !</h2>
 * <h2> Also check out {@link LcsWrapperAdapter} when refactoring </h2>
 */
@ApiStatus.Internal
public class LevelChunkSectionWrapper extends LevelChunkSection {
    
    private static final MethodHandle GET_STATES;
    private static final MethodHandle GET_BIOMES;
    private static final MethodHandle GET_NON_EMPTY_BLOCK_COUNT;
    private static final MethodHandle SET_NON_EMPTY_BLOCK_COUNT;
    private static final MethodHandle GET_TICKING_BLOCK_COUNT;
    private static final MethodHandle SET_TICKING_BLOCK_COUNT;
    private static final MethodHandle GET_TICKING_FLUID_COUNT;
    private static final MethodHandle SET_TICKING_FLUID_COUNT;
    private static final MethodHandle GET_SPECIAL_COLLIDING_BLOCKS;
    private static final MethodHandle SET_SPECIAL_COLLIDING_BLOCKS;
    private static final Field TICKING_BLOCKS;
    
    static {
        try {
            var lookup = MethodHandles.privateLookupIn(LevelChunkSection.class, MethodHandles.lookup());
            GET_STATES = lookup.findGetter(LevelChunkSection.class, "states", PalettedContainer.class);
            GET_BIOMES = lookup.findGetter(LevelChunkSection.class, "biomes", PalettedContainer.class);
            GET_NON_EMPTY_BLOCK_COUNT = lookup.findGetter(LevelChunkSection.class, "nonEmptyBlockCount", short.class);
            SET_NON_EMPTY_BLOCK_COUNT = lookup.findSetter(LevelChunkSection.class, "nonEmptyBlockCount", short.class);
            GET_TICKING_BLOCK_COUNT = lookup.findGetter(LevelChunkSection.class, "tickingBlockCount", short.class);
            SET_TICKING_BLOCK_COUNT = lookup.findSetter(LevelChunkSection.class, "tickingBlockCount", short.class);
            GET_TICKING_FLUID_COUNT = lookup.findGetter(LevelChunkSection.class, "tickingFluidCount", short.class);
            SET_TICKING_FLUID_COUNT = lookup.findSetter(LevelChunkSection.class, "tickingFluidCount", short.class);
            GET_SPECIAL_COLLIDING_BLOCKS = lookup.findGetter(LevelChunkSection.class, "specialCollidingBlocks", int.class);
            SET_SPECIAL_COLLIDING_BLOCKS = lookup.findSetter(LevelChunkSection.class, "specialCollidingBlocks", int.class);
            TICKING_BLOCKS = ReflectionUtils.getField(LevelChunkSection.class, "tickingBlocks");
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
    
    private final Level level;
    private final ChunkPos chunkPos;
    private final int bottomBlockY;
    private final LevelChunkSection delegate;
    private boolean migrationActive = false;
    
    @SuppressWarnings("unchecked")
    public LevelChunkSectionWrapper(Level level, ChunkPos chunkPos, int bottomBlockY, LevelChunkSection delegate) throws Throwable {
        super(
            (PalettedContainer<BlockState>) GET_STATES.invoke(delegate),
            (PalettedContainer<Holder<Biome>>) GET_BIOMES.invoke(delegate)
        );
        this.level = level;
        this.chunkPos = chunkPos;
        this.bottomBlockY = bottomBlockY;
        this.delegate = delegate instanceof LevelChunkSectionWrapper w ? w.delegate : delegate;
        recalcBlockCounts();
        ReflectionUtils.setFinalField$nova(TICKING_BLOCKS, this, TICKING_BLOCKS.get(delegate));
    }
    
    @Override
    public @NotNull BlockState setBlockState(int relX, int relY, int relZ, @NotNull BlockState state) {
        return setBlockState(relX, relY, relZ, state, true);
    }
    
    @Override
    public @NotNull BlockState setBlockState(int relX, int relY, int relZ, @NotNull BlockState state, boolean sync) {
        var pos = getBlockPos(relX, relY, relZ);
        
        if (state instanceof WrapperBlockState wrappedState) {
            WorldDataManager.INSTANCE.setBlockState(pos, wrappedState.getNovaState());
            return Blocks.AIR.defaultBlockState();
        }
        
        BlockState migrated = state;
        if (migrationActive) {
            migrated = BlockMigrator.migrateBlockState(pos, state);
        }
        
        var previous = delegate.setBlockState(relX, relY, relZ, migrated, sync);
        
        if (migrationActive) {
            BlockMigrator.handleBlockStatePlaced(pos, previous, state);
        }
        
        copyBlockCounts();
        return previous;
    }
    
    private BlockPos getBlockPos(int relX, int relY, int relZ) {
        return new BlockPos(
            level.getWorld(),
            relX + chunkPos.getMinBlockX(),
            relY + bottomBlockY,
            relZ + chunkPos.getMinBlockZ()
        );
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
    
    public int getBottomBlockY() {
        return bottomBlockY;
    }
    
    public boolean isMigrationActive() {
        return migrationActive;
    }
    
    public void setMigrationActive(boolean migrationActive) {
        this.migrationActive = migrationActive;
    }
    
    private void copyBlockCounts() {
        try {
            SET_NON_EMPTY_BLOCK_COUNT.invoke(this, GET_NON_EMPTY_BLOCK_COUNT.invoke(delegate));
            SET_TICKING_BLOCK_COUNT.invoke(this, GET_TICKING_BLOCK_COUNT.invoke(delegate));
            SET_TICKING_FLUID_COUNT.invoke(this, GET_TICKING_FLUID_COUNT.invoke(delegate));
            SET_SPECIAL_COLLIDING_BLOCKS.invoke(this, GET_SPECIAL_COLLIDING_BLOCKS.invoke(delegate));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    
}
