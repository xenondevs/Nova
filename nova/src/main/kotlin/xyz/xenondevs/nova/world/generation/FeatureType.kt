package xyz.xenondevs.nova.world.generation

import com.mojang.serialization.Codec
import net.minecraft.core.BlockPos
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration
import org.bukkit.Material
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.material.BlockNovaMaterial
import xyz.xenondevs.nova.util.item.nmsBlock

/**
 * An extension class of Minecraft's [Feature] that allows to use Bukkit's [Material] and [BlockNovaMaterial]s via
 * protected `setBlock` functions.
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class FeatureType<FC : FeatureConfiguration>(codec: Codec<FC>) : Feature<FC>(codec) {
    
    /**
     * Sets the block at the given position to the given [Material]. This method uses the `3` block change flag.
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
     * Sets the block at the given position to the given [BlockNovaMaterial].
     *
     * **Note: Nova blocks won't be properly placed/loaded until the chunk is loaded! (i.e. the chunk finished generating)**
     */
    protected fun setBlock(level: WorldGenLevel, pos: BlockPos, material: BlockNovaMaterial) {
        WorldDataManager.addOrphanBlock(level.level, pos.x, pos.y, pos.z, material)
    }
    
}
