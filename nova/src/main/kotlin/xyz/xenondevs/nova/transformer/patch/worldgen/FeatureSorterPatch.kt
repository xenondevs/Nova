package xyz.xenondevs.nova.transformer.patch.worldgen

import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.nova.transformer.MethodTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getMethodByName
import xyz.xenondevs.nova.world.generation.inject.biome.BiomeInjector

internal object FeatureSorterPatch : MethodTransformer(ReflectionRegistry.FEATURE_SORTER_BUILD_FEATURES_PER_STEP_METHOD, true) {
    
    override fun transform() {
        methodNode.instructions.insert(buildInsnList {
            addLabel()
            aLoad(0)
            invokeStatic(getMethodByName(BiomeInjector::class.java, false, "injectFeatures"))
        })
    }
    
}