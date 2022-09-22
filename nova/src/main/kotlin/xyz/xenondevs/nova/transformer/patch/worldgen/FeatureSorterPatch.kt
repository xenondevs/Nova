package xyz.xenondevs.nova.transformer.patch.worldgen

import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.nova.transformer.MethodTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.world.generation.WorldGenerationManager

internal object FeatureSorterPatch : MethodTransformer(ReflectionRegistry.FEATURE_SORTER_BUILD_FEATURES_PER_STEP_METHOD, true) {
    
    override fun transform() {
        methodNode.instructions.insert(buildInsnList {
            addLabel()
            aLoad(0)
            invokeStatic(WorldGenerationManager::class.internalName, "injectFeatures", "(Ljava/util/List;)V", false)
        })
    }
    
}