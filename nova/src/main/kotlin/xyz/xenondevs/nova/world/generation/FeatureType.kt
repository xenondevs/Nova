package xyz.xenondevs.nova.world.generation

import com.mojang.serialization.Codec
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.level.LevelWriter
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkGenerator
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration
import org.bukkit.Material
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.util.item.nmsBlock
import xyz.xenondevs.nova.world.block.NovaBlock
import java.util.function.Predicate

/**
 * An extension class of Minecraft's [Feature] that allows to use Bukkit's [Material] and [NovaBlock]s via
 * protected `setBlock` functions.
 */
@Suppress("MemberVisibilityCanBePrivate")
@ExperimentalWorldGen
abstract class FeatureType<FC : FeatureConfiguration>(codec: Codec<FC>) : Feature<FC>(codec) {
    
    /**
     * Sets the block at the given position to the given [Material]. This method uses the block change flag `3`.
     */
    protected fun setBlock(level: WorldGenLevel, pos: BlockPos, material: Material) {
        level.setBlock(pos, material.nmsBlock.defaultBlockState(), 3)
    }
    
    /**
     * Sets the block at the given position to the given [Material]. You can use the static constants defined
     * in Minecraft's [Block] class for the `flags` parameter. Check out [Sponge's constants class](https://github.com/SpongePowered/Sponge/blob/b146b4d66f5b150e1f1425b34c57c8b0c3624963/src/main/java/org/spongepowered/common/util/Constants.java#L942)
     * for more information. Generally it's recommended to use `3`/`2` as flags.
     */
    protected fun setBlock(level: WorldGenLevel, pos: BlockPos, material: Material, flags: Int) {
        level.setBlock(pos, material.nmsBlock.defaultBlockState(), flags)
    }
    
    /**
     * Sets the block at the given position to the given [NovaBlock].
     *
     * **Note: Nova blocks won't be properly placed/loaded until the chunk is loaded! (i.e. the chunk finished generating)**
     */
    protected fun setBlock(level: WorldGenLevel, pos: BlockPos, material: NovaBlock) {
        WorldDataManager.addOrphanBlock(level.level, pos.x, pos.y, pos.z, material)
    }
    
    //<editor-fold desc="Overrides for better param names" defaultstate="collapsed">
    
    abstract override fun place(ctx: FeaturePlaceContext<FC>): Boolean
    
    override fun setBlock(level: LevelWriter, pos: BlockPos, state: BlockState) {
        super.setBlock(level, pos, state)
    }
    
    override fun safeSetBlock(level: WorldGenLevel, pos: BlockPos, state: BlockState, predicate: Predicate<BlockState>) {
        super.safeSetBlock(level, pos, state, predicate)
    }
    
    override fun place(config: FC, level: WorldGenLevel, generator: ChunkGenerator, random: RandomSource, pos: BlockPos): Boolean {
        return super.place(config, level, generator, random, pos)
    }
    
    override fun markAboveForPostProcessing(level: WorldGenLevel, pos: BlockPos) {
        super.markAboveForPostProcessing(level, pos)
    }
    
    //</editor-fold>
}
