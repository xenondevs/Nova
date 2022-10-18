package xyz.xenondevs.nova.world.generation

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.data.DataFileParser
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.transformer.Patcher
import xyz.xenondevs.nova.util.NMSUtils
import xyz.xenondevs.nova.util.NMSUtils.REGISTRY_ACCESS
import xyz.xenondevs.nova.world.generation.inject.codec.CodecOverride
import xyz.xenondevs.nova.world.generation.inject.codec.blockstate.BlockStateCodecOverride
import xyz.xenondevs.nova.world.generation.registry.FeatureRegistry
import xyz.xenondevs.nova.world.generation.registry.WorldGenRegistry
import java.io.File

internal object WorldGenManager : Initializable() {
    
    override val initializationStage = InitializationStage.PRE_WORLD
    override val dependsOn = setOf(Patcher, Resources, AddonsInitializer, DataFileParser)
    
    val WORLD_GEN_DIR = File(NOVA.dataFolder, ".data/worldgen")
    
    private val WORLD_GEN_REGISTRIES = listOf(FeatureRegistry)
    private val NMS_REGISTRIES = WORLD_GEN_REGISTRIES.asSequence()
        .flatMap { it.neededRegistries }
        .associateWithTo(Object2ObjectOpenHashMap()) { REGISTRY_ACCESS.registry(it).get() }
    private val CODEC_OVERRIDES by lazy { listOf(BlockStateCodecOverride) }
    
    override fun init() {
        WORLD_GEN_REGISTRIES.forEach(WorldGenRegistry::loadFiles)
        NMS_REGISTRIES.values.forEach(NMSUtils::unfreezeRegistry)
        CODEC_OVERRIDES.forEach(CodecOverride::replace)
        WORLD_GEN_REGISTRIES.forEach { it.register(REGISTRY_ACCESS) }
        NMS_REGISTRIES.values.forEach(NMSUtils::freezeRegistry)
    }
    
}