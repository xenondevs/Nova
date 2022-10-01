package xyz.xenondevs.nova.world.generation

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.core.Registry
import net.minecraft.data.BuiltinRegistries
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.transformer.Patcher
import xyz.xenondevs.nova.util.NMSUtils
import xyz.xenondevs.nova.util.NMSUtils.REGISTRY_ACCESS
import xyz.xenondevs.nova.util.data.decodeJsonFile
import xyz.xenondevs.nova.util.data.getFirstValueOrThrow
import xyz.xenondevs.nova.world.generation.inject.biome.BiomeInjector
import xyz.xenondevs.nova.world.generation.inject.codec.CodecOverride
import xyz.xenondevs.nova.world.generation.inject.codec.blockstate.BlockStateCodecOverride
import java.io.File

internal object WorldGenManager : Initializable() {
    
    override val initializationStage = InitializationStage.PRE_WORLD
    override val dependsOn = setOf(Patcher, Resources, AddonsInitializer)
    
    val WORLD_GEN_DIR = File(NOVA.dataFolder, ".data/worldgen")
    val REGISTRIES = setOf(
        Registry.CONFIGURED_FEATURE_REGISTRY,
        Registry.PLACED_FEATURE_REGISTRY
    ).associateWithTo(Object2ObjectOpenHashMap()) { REGISTRY_ACCESS.registry(it).get() }
    
    private val CODEC_OVERRIDES by lazy { listOf(BlockStateCodecOverride) }
    
    override fun init() {
        WorldGenFiles.extract()
        REGISTRIES.values.forEach(NMSUtils::unfreezeRegistry)
        CODEC_OVERRIDES.forEach(CodecOverride::replace)
        
        BiomeInjector.parseInjections()
        
        REGISTRIES.values.forEach(NMSUtils::freezeRegistry)
    }
    
    private fun registerFeatureConfiguration(file: File, name: NamespacedId) {
        val configuredFeature = ConfiguredFeature.CODEC
            .decodeJsonFile(file)
            .getFirstValueOrThrow("Failed to parse feature configuration of $name")
        
        BuiltinRegistries.registerExact(REGISTRIES[Registry.CONFIGURED_FEATURE_REGISTRY], name.toString(":"), configuredFeature)
    }
    
    private fun registerPlacedFeature(file: File, name: NamespacedId) {
        val placedFeature = PlacedFeature.CODEC
            .decodeJsonFile(file)
            .getFirstValueOrThrow("Failed to parse placed feature of $name")
        
        BuiltinRegistries.registerExact(REGISTRIES[Registry.PLACED_FEATURE_REGISTRY], name.toString(":"), placedFeature)
    }
    
}