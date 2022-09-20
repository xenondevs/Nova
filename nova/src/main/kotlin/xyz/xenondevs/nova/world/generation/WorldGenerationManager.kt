package xyz.xenondevs.nova.world.generation

import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.transformer.Patcher
import xyz.xenondevs.nova.world.generation.codec.BlockStateCodecOverride
import xyz.xenondevs.nova.world.generation.codec.CodecOverride

internal object WorldGenerationManager : Initializable() {
    
    override val initializationStage = InitializationStage.PRE_WORLD
    override val dependsOn = setOf(Patcher, Resources)
    
    private val CODEC_OVERRIDES = listOf(BlockStateCodecOverride)
    
    override fun init() {
        CODEC_OVERRIDES.forEach(CodecOverride::replace)
    }
    
}