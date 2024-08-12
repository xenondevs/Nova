package xyz.xenondevs.nova.patch.impl.worldgen

import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.FeatureSorter
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.nova.patch.MethodTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.world.generation.inject.biome.BiomeInjector
import java.util.function.Function

private val BUILD_FEATURES_PER_STEP = ReflectionUtils.getMethod(
    FeatureSorter::class,
    "buildFeaturesPerStep",
    List::class, Function::class, Boolean::class
)

/**
 * Allows the injection of additional features into a [Biome].
 */
internal object FeatureSorterPatch : MethodTransformer(BUILD_FEATURES_PER_STEP, true) {
    
    override fun transform() {
        methodNode.instructions.insert(buildInsnList {
            addLabel()
            aLoad(0)
            invokeStatic(BiomeInjector::injectFeatures)
        })
    }
    
}