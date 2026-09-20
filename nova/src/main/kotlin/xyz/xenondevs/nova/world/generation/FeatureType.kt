package xyz.xenondevs.nova.world.generation

import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.chunk.ChunkGenerator
import net.minecraft.world.level.levelgen.feature.Feature
import org.bukkit.Material
import org.bukkit.block.BlockType
import xyz.xenondevs.nova.util.nmsBlock
import xyz.xenondevs.nova.world.block.NovaBlock

/**
 * An extension class of Minecraft's [Feature] that allows to use Bukkit's [Material] and [NovaBlock]s via
 * protected `setBlock` functions.
 */
@Suppress("MemberVisibilityCanBePrivate")
@ExperimentalWorldGen
abstract class FeatureType(private val featureCodec: MapCodec<out Feature>) : Feature {
    
    /**
     * Sets the block at the given position to the given [Material]. This method uses the block change flag `3`.
     */
    protected fun setBlock(level: WorldGenLevel, pos: BlockPos, type: BlockType) {
        level.setBlock(pos, type.nmsBlock.defaultBlockState(), 3)
    }
    
    /**
     * Sets the block at the given position to the given [Material]. You can use the static constants defined
     * in Minecraft's [Block] class for the `flags` parameter. Check out [Sponge's constants class](https://github.com/SpongePowered/Sponge/blob/b146b4d66f5b150e1f1425b34c57c8b0c3624963/src/main/java/org/spongepowered/common/util/Constants.java#L942)
     * for more information. Generally it's recommended to use `3`/`2` as flags.
     */
    protected fun setBlock(level: WorldGenLevel, pos: BlockPos, type: BlockType, flags: Int) {
        level.setBlock(pos, type.nmsBlock.defaultBlockState(), flags)
    }
    
    override fun codec(): MapCodec<out Feature> = featureCodec
    
    abstract override fun place(level: WorldGenLevel, generator: ChunkGenerator, random: RandomSource, pos: BlockPos): Boolean
}
